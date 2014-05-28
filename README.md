# Cool side project

[] need to work on bitcoind framework
[] need to put server config files into a repo
[] work on hedged functionality - backlog
[] need to check the jdbc returns
[] need to start on admin functionality
[] add fields to the schema to support hedged listing
[] add a escrow entry for our fee - remember our fee needs to include the blockchain transaction fee.


sudo apt-get install bitcoind

## Prerequisites

You will need [Leiningen][1] 2.0 or above installed. You need [Postgresql][2] 9.1.10
and you will need [couchbase][3] 2.2.0

[1]: https://github.com/technomancy/leiningen
[2]: https://help.ubuntu.com/community/PostgreSQL
[3]: http://www.couchbase.com/docs//couchbase-manual-2.0/couchbase-getting-started-install-ubuntu.html

## Running

To check on the application stats visit http://ubuntu:8091/index.html

To start a web server for the application, run:

    lein ring server

## License

Copyright © 2013 FIXME
