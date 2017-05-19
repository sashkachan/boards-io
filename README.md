# boards-io
A very stripped clone of a product that rhymes with "Hèllo"

# Stack

This is an example project that explores how to create a single page app with authentication, database storage and om.next

# How to run

First, edit ```env/dev/resources/config.edn``` to add Google Authenticator API credentials.
```
{:client-id "GOOG-CLIENTID"
 :client-secret "GOOG-CLIENT-SECRET"
 :redirect-uri "http://localhost:9082/auth"
 :uri "datomic:mem://boards"
 :port 9082}
```

Then, from projects directory:
```
lein run
```

Navigate to ```http://localhost:9082/``` in your browser.

# Deployment

Under ./deployment you'll find various k8s deployment files.
This will automatically handle the certificates creation using http://letsencrypt.org

