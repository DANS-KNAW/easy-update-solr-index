easy-update-solr-index
======================

Update EASY's SOLR Search Index with metadata of datasets in EASY's Fedora Commons Repository.

SYNOPSIS
--------

    easy-update-solr-index [<option>...] -q <fcrepo-query>...
    easy-update-solr-index [<option>...] -d <dataset-id>...
    easy-update-solr-index [<option>...] --file <text-file-with-dataset-id-per-line>


DESCRIPTION
-----------

The EASY SOLR Search Index is a configured [Apache SOLR Service] that contains an index of the metadata stored
in the Fedora Commons Repository at the dataset level. This command extracts the required metadata from the 
Fedora Commons Repository and sends it to the EASY SOLR Search Index to add or update the record for the specified
dataset(s)


ARGUMENTS
---------

      -b, --dataset-batch-size  <arg>   Number of datasets to read at once from the
                                        dataset-query (default = 100)
      -i, --dataset-id  <arg>...        ID of dataset to update, for eaxample:
                                        easy-dataset:1
      -t, --dataset-timeout  <arg>      Milliseconds to pause after processing a
                                        dataset to avoid reducing performance of the
                                        production system too much (default = 1000)
      -d, --debug                       If specified: only generate document(s), do
                                        not send anything to SOLR
      -p, --fcrepo-password  <arg>      Password for fcrepo-user (default = )
      -q, --fcrepo-query  <arg>...      Fedora query that selects datasets, query
                                        example: 'pid~easy-dataset:*'. see also help
                                        for 'specific fields' on
                                        <fcrepo-server>/objects
      -f, --fcrepo-server  <arg>        URL of Fedora Commons Repository Server to
                                        connect to
                                        (default = http://localhost:8080/fedora)
      -u, --fcrepo-user  <arg>          User to connect to fcrepo-server (default = )
          --file  <arg>                 Text file with a dataset-id per line
      -o, --output                      If provided: output SOLR document(s) to stdout
      -s, --solr-update-url  <arg>      URL to POST SOLR documents to
                                        (default = http://localhost:8080/solr)
          --help                        Show help message
          --version                     Show version of this program



INSTALLATION AND CONFIGURATION
------------------------------

### Installation steps:

1. Unzip the tarball to a directory of your choice, e.g. /opt/
2. A new directory called easy-update-solr-index-<version> will be created
3. The directory from step 2 is used as value for the system variabele ``app.home``
4. Add ``${app.home}/bin`` to your ``PATH`` environment variable


### Configuration

Set defaults for the command line arguments in ``${app.home}/cfg/application.properties``.
Omitted items fall bach to the documented defaults.

Configure logging in ``${app.home}/cfg/logback.xml`` which is self explaining.


BUILDING FROM SOURCE
--------------------

Prerequisites:

* Java 8 or higher
* Maven 3.3.3 or higher
 
Steps:

1. Clone and build the [dans-parent] project (*can be skipped if you have access to the DANS maven repository*)
      
        git clone https://github.com/DANS-KNAW/dans-parent.git
        cd dans-parent
        mvn install
2. Clone and build this project

        git clone https://github.com/DANS-KNAW/easy-update-solr-index.git
        cd easy-update-solr-index
        mvn install


[Apache SOLR Service]: https://lucene.apache.org/solr/
