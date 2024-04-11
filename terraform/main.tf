/**
 * BOOTSTRAPPING
 * Sadly, there are still manual steps needed to bootstrap a terraform configuration (See also https://github.com/terraform-google-modules/terraform-example-foundation/blob/master/0-bootstrap/README.md)
 *
 * - create var.project in GCP
 * - enable billing
 * - create service account and grant:
 *    - "Editor"
 *    - "Cloud Run Admin" role
 *    - "App Engine Admin" + "App Engine Creator" role (for DataStore)
 *    (This might be fine tuned in the future)
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
variable "gandi_access_token" {
  type = string
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

module "gandi" {
  source = "./modules/gandi"

  loadbalancer_ip = google_compute_global_address.default.address
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

provider google-beta {
  project = var.project
  region  = var.region
}

resource "google_project_service" "api_compute" {
  provider = google-beta
  service = "compute.googleapis.com"
}

resource "google_project_service" "api_iam" {
  provider = google-beta
  service = "iam.googleapis.com"
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
    default_url_redirect {
      https_redirect = true
      host_redirect = "androidmakers.droidcon.com"
      path_redirect = "/"
      strip_query = false
      redirect_response_code = "MOVED_PERMANENTLY_DEFAULT"
    }


    path_rule {
      paths = [
        "/graphql",
        "/sandbox/*",
        "/openfeedback.json",
      ]
      service = google_compute_backend_service.service.id
    }
  }
}

resource "google_compute_backend_bucket" "static_content" {
  provider    = google-beta
  name        = "static-content"
  bucket_name = google_storage_bucket.static_content.name
  enable_cdn  = true
}

resource "google_compute_backend_service" "service" {
  provider   = google-beta
  name       = "service"
  enable_cdn = true

  custom_response_headers = ["X-Cache-Hit: {cdn_cache_status}"]

  log_config {
    enable      = true
    sample_rate = 1
  }

  backend {
    group = google_compute_region_network_endpoint_group.cloudrunservice.id
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


resource "google_compute_region_network_endpoint_group" "cloudrunservice" {
  provider              = google-beta
  name                  = "cloudrunservice"
  region                = var.region
  network_endpoint_type = "SERVERLESS"

  cloud_run {
    service = "service"
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

resource "google_artifact_registry_repository" "service-images" {
  repository_id = "service-images"
  provider      = google-beta
  description   = "images for the AndroidMakers API"
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

resource "google_service_account" "cloudrun_service_identity" {
  provider = google-beta
  account_id = "my-service-account"
}

resource "google_cloud_run_v2_service" "service" {
  name     = "service"
  provider = google-beta
  ingress  = "INGRESS_TRAFFIC_ALL"
  location = var.region


  template {
    containers {
      image = "europe-west9-docker.pkg.dev/androidmakers-a6883/service-images/service"
      resources {
        cpu_idle = true
        startup_cpu_boost = true
      }
      env {
        name = "GOOGLE_CLOUD_PROJECT"
        value = var.project
      }
    }
    service_account = google_service_account.cloudrun_service_identity.email
  }
}

resource "google_cloud_run_service_iam_binding" "graphql" {
  provider = google-beta
  location = google_cloud_run_v2_service.service.location
  service  = google_cloud_run_v2_service.service.name
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

# It's an appending app but we're only using datastore
resource "google_app_engine_application" "datastore" {
  project = var.project
  location_id = "europe-west"
  database_type = "CLOUD_DATASTORE_COMPATIBILITY"
}

output "ip_addr" {
  value = google_compute_global_address.default.address
}
