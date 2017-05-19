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

# Images

![Auth](https://www.dropbox.com/s/o1pme6j0c0eizda/Auth.png?dl=1)
![Boards](https://www.dropbox.com/s/vy78zslrxqphkcn/Boards.png?dl=1)
![Drag](https://www.dropbox.com/s/hc4p7pggv82m8sm/Drag.png?dl=1)
![DragCol](https://www.dropbox.com/s/vm7jtaz59boabyh/DragCol.png?dl=1)
![NewCol](https://www.dropbox.com/s/wqngiod3uw0wv4e/NewCol.png?dl=1)
