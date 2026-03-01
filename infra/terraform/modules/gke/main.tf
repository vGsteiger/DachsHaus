# GKE Module - Placeholder
# GKE cluster with private nodes, Workload Identity, etcd encryption, binary auth, Dataplane V2

resource "google_container_cluster" "primary" {
  name     = "dachshaus-cluster"
  location = var.region
  network  = var.network_id
  subnetwork = var.subnet_id
}
