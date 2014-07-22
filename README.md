# Cool side project

[] need to work on bitcoind framework
[] need to put server config files into a repo
[] need to start on admin functionality

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

Future Enhancements
•	Implement Bitmessage Address field, and add bitmessage server.
•	Verified Vendor fee.
•	Permalinks for vendors, /user/alias
•	http://directory4iisquf.onion add verified vendor directory, for pgp keys.
•	Contract support – send message
•	Allow Vendors to post to followers.
•	Add latest posts to user page.
•	Add an option to encrypt all messages to a user.
 
