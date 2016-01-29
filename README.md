easy-update-solr-index
======================
[![Build Status](https://travis-ci.org/DANS-KNAW/easy-update-solr-index.png?branch=master)](https://travis-ci.org/DANS-KNAW/easy-update-solr-index)

Update EASY's SOLR Search Index with metadata of datasets in EASY's Fedora Commons Repository.

SYNOPSIS
--------

    easy-update-solr-index [<option>...] [ <dataset-id> | <fcrepo-query> | <text-file> ] ...


DESCRIPTION
-----------

The EASY SOLR Search Index is a configured [Apache SOLR Service] that contains an index of the metadata stored
in the Fedora Commons Repository at the dataset level. This command extracts the required metadata from the 
Fedora Commons Repository and sends it to the EASY SOLR Search Index to add or update the record for the specified
dataset(s)


ARGUMENTS
---------

     -b, --dataset-batch-size  <arg>   Number of datasets to update at once,
                                       maximized by fedora to 100 when selecting
                                       datasets with a query
     -t, --dataset-timeout  <arg>      Milliseconds to pause after processing a batch
                                       of datasets to avoid reducing performance of
                                       the production system too much
     -d, --debug                       If specified: only generate document(s), do
                                       not send anything to SOLR
     -p, --fcrepo-password  <arg>      Password for fcrepo-user
     -f, --fcrepo-server  <arg>        URL of Fedora Commons Repository Server to
                                       connect to
     -u, --fcrepo-user  <arg>          User to connect to fcrepo-server
     -o, --output                      If provided: output SOLR document(s) to stdout
     -s, --solr-update-url  <arg>      URL to POST SOLR documents to
         --help                        Show help message
         --version                     Show version of this program
   
    trailing arguments:
     dataset-ids (required)   One or more of: dataset id (for example
                              'easy-dataset:1'), a file with a dataset id per line or
                              a fedora query that selects datasets (for example
                              'pid~easy-dataset:*', see also help for 'specific
                              fields' on <fcrepo-server>/objects) 



INSTALLATION AND CONFIGURATION
------------------------------

### Installation steps:

1. Unzip the tarball to a directory of your choice, e.g. /opt/
2. A new directory called easy-update-solr-index-<version> will be created
3. The directory from step 2 is used as value for the system property ``app.home``
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

        git clone https://github.com/DANS-KNAW/easy-update-solr-index.git
        cd easy-update-solr-index
        mvn install


[Apache SOLR Service]: https://lucene.apache.org/solr/
