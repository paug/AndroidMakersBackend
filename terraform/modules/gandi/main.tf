terraform {
  required_providers {
    gandi = {
      version = "2.3.0"
      source   = "go-gandi/gandi"
    }
  }
}

provider "gandi" {
  personal_access_token = "79f3b900dfbc3e55a195060f56bb2747d82d54ba"
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

resource "gandi_livedns_domain" "androidmakers_fr" {
  name = "androidmakers.fr"
}

resource "gandi_livedns_record" "androidmakers_fr" {
  zone = gandi_livedns_domain.androidmakers_fr.id
  name = "@"
  type = "A"
  ttl = 3600
  values = [
    var.loadbalancer_ip
  ]
}