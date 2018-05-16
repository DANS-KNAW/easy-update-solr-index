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

     -b, --dataset-batch-size  <arg>   Number of datasets to update at once, maximized by fedora to 100 when
                                       selecting datasets with a query (default = 100)
     -t, --dataset-timeout  <arg>      Milliseconds to pause after processing a batch of datasets to avoid
                                       reducing performance of the production system too much (default = 1000)
     -d, --debug                       If specified: only generate document(s), do not send anything to SOLR
     -o, --output                      If provided: output SOLR document(s) to stdout
     -h, --help                        Show help message
     -v, --version                     Show version of this program

    trailing arguments:
     dataset-ids (required)   One or more of: dataset id (for example 'easy-dataset:1'), a file with a dataset id
                              per line or a fedora query that selects datasets (for example 'pid~easy-dataset:*',
                              see also help for 'specific fields' on
                              http://deasy.dans.knaw.nl:8080/fedora/objects)

Note that the actual defaults (shown with `--help`) depend on the actual configuration.


INSTALLATION AND CONFIGURATION
------------------------------


1. Unzip the tarball to a directory of your choice, typically `/usr/local/`
2. A new directory called easy-update-solr-index-<version> will be created
3. Add the command script to your `PATH` environment variable by creating a symbolic link to it from a directory that is
   on the path, e.g. 
   
        ln -s /usr/local/easy-update-solr-index-<version>/bin/easy-update-solr-index /usr/bin



General configuration settings can be set in `cfg/application.properties` and logging can be configured
in `cfg/logback.xml`. The available settings are explained in comments in aforementioned files.


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
