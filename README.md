# dynamoplus-e2e-local-tests
Java Junit project to run e2e tests on a dynamoplus local installation

## Dependencies

- dynamoplus running instance

Note: 
Dynamoplus could be installed either on localhost or on AWS. Running the test against an AWS installation you might encours in extra costs depending on the workload and your configuration.




# Usage



First set the environment variables:

- DYNAMOPLUS_HOST
    
    default: http://localhost:3000
    
     e.g.: http://dynamolpus:3000  
- DYNAMOPLUS_ROOT
    
    default: root
    
    e.g.: admin
    
    
- DYNAMOPLUS_PASSWORD: password

    default: 12345
    
    e.g.: password


Then run:

```bashs
mvn clean test 
```

to run all the test suite


