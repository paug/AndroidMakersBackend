/**
 * BOOTSTRAPPING
 * Sadly, there are still manual steps needed to bootstrap a terraform configuration (See also https://github.com/terraform-google-modules/terraform-example-foundation/blob/master/0-bootstrap/README.md)
 *
 * - create var.project in GCP
 * - enable billing
 * - create service account and grant "Editor" + "Cloud Run Admin" role. This might be fine tuned in the future
 * - export the service account key in GOOGLE_APPLICATION_CREDENTIALS_CONTENT
 * - create "androidmakers-tfstate" bucket
 * - create var.domain in Gandi
 * - create Gandi access token
 * - export the access token in TF_VAR_gandi_access_token
 */
// These resources must be created manually before the first terraform apply
variable "project" {
  default = "androidmakers-a6883"
}
variable "domain" {
  default = "androidmakers.fr"
}
//
variable "gandi_access_token" {
  type = string
}

# Also create "androidmakers-tfstate" as it can sadly not be a variable
# Typically use the same resource as for tfstate-bucket above (but doest have to)
variable "region" {
  default = "europe-west9"
}

terraform {
  backend "gcs" {
    bucket = "androidmakers-tfstate"
    prefix = "terraform/state"
  }
}

terraform {
  required_providers {
    gandi = {
      version = "2.3.0"
      source   = "go-gandi/gandi"
    }
  }
}

provider "gandi" {
  personal_access_token = var.gandi_access_token
}

import {
  id = "androidmakers.fr"
  to = gandi_domain.androidmakers_fr
}

resource "gandi_domain" "androidmakers_fr" {
  name = "androidmakers.fr"

  # The owner block is required even though sadly it's not supported by the current provider
  owner {
    email = "placeholder"
    type = "company"
    street_addr = "placeholder"
    zip = "placeholder"
    phone = "placeholder"
    given_name = "placeholder"
    family_name = "placeholder"
    country = "FR"
    city = "placeholder"
    state = "placeholder"
    mail_obfuscated = true
    organisation = "placeholder"
    data_obfuscated = true
  }
  lifecycle {
    ignore_changes = [
      # "Error: domain owner contact update is currently not supported"
      owner,
    ]
  }
}

import {
  id = "androidmakers.fr"
  to = gandi_livedns_domain.androidmakers_fr
}

resource "gandi_livedns_domain" "androidmakers_fr" {
  name = "androidmakers.fr"
}

import {
  id = "androidmakers.fr/@/A"
  to = gandi_livedns_record.androidmakers_fr
}

resource "gandi_livedns_record" "androidmakers_fr" {
  zone = gandi_livedns_domain.androidmakers_fr.id
  name = "@"
  type = "A"
  ttl = 3600
  values = [
    google_compute_global_address.default.address
  ]
}


provider google-beta {
  project = var.project
  region  = var.region
}

resource "google_project_service" "api_compute" {
  provider = google-beta
  service = "compute.googleapis.com"
}

resource "google_project_service" "api_artifact_registry" {
  provider = google-beta
  service = "artifactregistry.googleapis.com"
}

resource "google_project_service" "api_cloud_run" {
  provider = google-beta
  service = "run.googleapis.com"
}

resource "google_compute_url_map" "default" {
  name            = "default"
  provider        = google-beta
  default_service = google_compute_backend_bucket.static_content.id

  host_rule {
    hosts        = [var.domain]
    path_matcher = "default"
  }

  path_matcher {
    name            = "default"
    default_service = google_compute_backend_bucket.static_content.id

    path_rule {
      paths = [
        "/graphiql",
        "/graphql",
        "/images/*"
      ]
      service = google_compute_backend_service.graphql.id
    }
    path_rule {
      paths = [
        "/update/*"
      ]
      service = google_compute_backend_service.import.id
    }
  }
}

resource "google_compute_backend_bucket" "static_content" {
  provider    = google-beta
  name        = "static-content"
  bucket_name = google_storage_bucket.static_content.name
  enable_cdn  = true
}

resource "google_compute_backend_service" "graphql" {
  provider   = google-beta
  name       = "graphql"
  enable_cdn = true

  custom_response_headers = ["X-Cache-Hit: {cdn_cache_status}"]

  log_config {
    enable      = true
    sample_rate = 1
  }

  backend {
    group = google_compute_region_network_endpoint_group.cloudrungraphql.id
  }

  cdn_policy {
    cache_mode = "USE_ORIGIN_HEADERS"

    cache_key_policy {
      include_protocol     = true
      include_host         = true
      include_query_string = true
      include_http_headers = ["conference"]
    }
  }
  compression_mode = "DISABLED"
}

resource "google_compute_backend_service" "import" {
  provider   = google-beta
  name       = "import"
  enable_cdn = true

  custom_response_headers = ["X-Cache-Hit: {cdn_cache_status}"]

  log_config {
    enable      = true
    sample_rate = 1
  }

  backend {
    group = google_compute_region_network_endpoint_group.cloudrunimport.id
  }

  cdn_policy {
    cache_mode = "USE_ORIGIN_HEADERS"

    cache_key_policy {
      include_protocol     = true
      include_host         = true
      include_query_string = true
      include_http_headers = ["conference"]
    }
  }
  compression_mode = "DISABLED"
}

resource "google_compute_region_network_endpoint_group" "cloudrungraphql" {
  provider              = google-beta
  name                  = "cloudrungraphql"
  region                = var.region
  network_endpoint_type = "SERVERLESS"

  cloud_run {
    service = "graphql"
  }
}

resource "google_compute_region_network_endpoint_group" "cloudrunimport" {
  provider              = google-beta
  name                  = "cloudrunimport"
  region                = var.region
  network_endpoint_type = "SERVERLESS"

  cloud_run {
    service = "import"
  }
}

resource "google_compute_managed_ssl_certificate" "default2" {
  name     = "default2"
  provider = google-beta

  managed {
    domains = [var.domain]
  }
}

resource "google_compute_global_address" "default" {
  provider = google-beta
  name     = "default"
}

resource "google_compute_target_https_proxy" "default" {
  provider         = google-beta
  name             = "default"
  url_map          = google_compute_url_map.default.id
  ssl_certificates = [google_compute_managed_ssl_certificate.default2.id]
}

resource "google_compute_target_http_proxy" "default" {
  provider = google-beta
  name     = "default"
  url_map  = google_compute_url_map.default.id
}

resource "google_compute_global_forwarding_rule" "https" {
  name                  = "https"
  provider              = google-beta
  ip_protocol           = "TCP"
  load_balancing_scheme = "EXTERNAL"
  port_range            = "443"
  target                = google_compute_target_https_proxy.default.id
  ip_address            = google_compute_global_address.default.id
}

resource "google_compute_global_forwarding_rule" "http" {
  name                  = "http"
  provider              = google-beta
  ip_protocol           = "TCP"
  load_balancing_scheme = "EXTERNAL"
  port_range            = "80"
  target                = google_compute_target_http_proxy.default.id
  ip_address            = google_compute_global_address.default.id
}

resource "google_artifact_registry_repository" "graphql-images" {
  repository_id = "graphql-images"
  provider      = google-beta
  description   = "images for the GraphQL API"
  format        = "DOCKER"
  cleanup_policies {
    id     = "keep-minimum-versions"
    action = "KEEP"
    most_recent_versions {
      # Delete old images automatically
      keep_count = 5
    }
  }
}

resource "google_cloud_run_v2_service" "graphql" {
  name     = "graphql"
  provider = google-beta
  ingress  = "INGRESS_TRAFFIC_ALL"
  location = var.region


  template {
    containers {
      image = "us-docker.pkg.dev/cloudrun/container/placeholder"
      resources {
        cpu_idle = true
        startup_cpu_boost = true
      }
    }
  }
}

resource "google_cloud_run_service_iam_binding" "graphql" {
  provider = google-beta
  location = google_cloud_run_v2_service.graphql.location
  service  = google_cloud_run_v2_service.graphql.name
  role     = "roles/run.invoker"
  members  = [
    "allUsers"
  ]
}

resource "google_artifact_registry_repository" "import-images" {
  repository_id = "import-images"
  provider      = google-beta
  description   = "images for the Import API"
  format        = "DOCKER"
  cleanup_policies {
    id     = "keep-minimum-versions"
    action = "KEEP"
    most_recent_versions {
      # Delete old images automatically
      keep_count = 5
    }
  }
}

resource "google_cloud_run_v2_service" "import" {
  name     = "import"
  provider = google-beta
  ingress  = "INGRESS_TRAFFIC_ALL"
  location = var.region

  template {
    containers {
      image = "us-docker.pkg.dev/cloudrun/container/placeholder"
      resources {
        cpu_idle = true
        startup_cpu_boost = true
      }
    }
  }
}

resource "google_cloud_run_service_iam_binding" "import" {
  provider = google-beta
  location = google_cloud_run_v2_service.import.location
  service  = google_cloud_run_v2_service.import.name
  role     = "roles/run.invoker"
  members  = [
    "allUsers"
  ]
}

# This was created outside of terraform, import it
import {
  id = "androidmakers-tfstate"
  to = google_storage_bucket.tfstate
}

resource "google_storage_bucket" "tfstate" {
  provider      = google-beta
  name          = "androidmakers-tfstate"
  force_destroy = false
  location      = var.region
  storage_class = "STANDARD"
  versioning {
    enabled = true
  }
}

resource "google_storage_bucket" "static_content" {
  provider      = google-beta
  name          = "androidmakers-static-content"
  # This bucket was created before everything was in terraform and uses a multi-region instead of var.region
  location      = "US"
  storage_class = "STANDARD"

  website {
    main_page_suffix = "index.html"
    not_found_page   = "404.html"
  }
}

output "ip_addr" {
  value = google_compute_global_address.default.address
}
