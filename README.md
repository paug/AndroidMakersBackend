# Android Makers Backend

You can explore the API using the [playground](https://androidmakers-2023.ew.r.appspot.com/playground):

```
https://androidmakers-2023.ew.r.appspot.com/playground
```

For an example, the following query returns the 100 first sessions:

```graphql
query {
  sessions(first: 100) {
    nodes {
      id
      title
      speakers {
        name
      }
    }
  }
}
```

### Authentication

Setting and reading bookmarks requires to be authenticated. Authentication is handled through [Firebase authentication](https://firebase.google.com/docs/auth). Retrieve an id token with `Firebase.auth.currentUser?.getIdToken()` and pass it in your "Authorization" headers:

```
Authorization: Bearer ${firebaseIdToken}
```


### Changing data:

You can modify data in [service-graphql/src/main/resources/data.json](service-graphql/src/main/resources/data.json). Changes are deployed when pushed to main

### Running locally: 

To run locally (requires Google Cloud service account key): 

```
./gradlew bootRun
```