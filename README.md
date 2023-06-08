# mulog-prod-file

[μ/log](https://github.com/BrunoBonacci/mulog) file publisher for in production environments.

## Usage

```clojure
(require '[com.brunobonacci.mulog :as μ]
         '[jeroenvandijk.mulog.publishers.prod-file])
         
(def stop-pub (μ/start-publisher!
                 {:type :multi
                  :publishers
                  [{:type :console}
                   {:type :prod-file
                    :filename "tmp/app.log"
                    :max-file-count 20
                    :max-byte-count (* 100 1024) ;; 100kb per file
                    }]}))
                    
 (dotimes [i 10000]
    (μ/log ::hello :to "New World!"))      
    
(stop-pub)
```

## Installation 

In `deps.edn`
```clojure
{:deps {
   io.github.jeroenvandijk/mulog-prod-file {:git/sha "HEAD"}
}}
```


## Features

- [X] File rotation
- [X] EDN support


## License

Copyright © 2023 Jeroen van Dijk -  Distributed under the [Apache License v2.0](http://www.apache.org/licenses/LICENSE-2.0)

This project contains code from Timbre (rotor.clj) which is licensed under the EPL 1.0.
