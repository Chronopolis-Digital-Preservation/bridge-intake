# Bridge Intake

The bridge-intake service performs curatorial steps in order to ingest data
from the duracloud bridge into Chronopolis.

## Installation

The service can be packaged as an rpm in order to be installed on rhel servers.
Under the `rpm` directory, run the `build.sh` script to generate the rpm. 
`rpmbuild` is required as a dependency.

## Internals

As mentioned above, the `bridge-intake` service handles curation of content in
order to have it conform to Chronopolis' needs. These are:

* Bagging
  * handled primarily through the BaggingTasklet and `bagger` library
* Tokenizing
  * from `tokenizer-mq`

Otherwise most of the interaction is in dealing with the Duracloud Bridge API
and the Chronopolis Ingest API. On a successful replication of a snapshot, the
`bridge-intake` service will clean up the Bag which was created.

# Releases

Releases are handled in the same manner as other Chronopolis software. The CI
pipeline will run deploy tasks for both the maven artifacts and rpms when a tag
is pushed. The releases will need to be marked as a `RELEASE` within maven in 
order for the artifact to be deployed to the repository.
