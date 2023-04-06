# Android Makers Backend

You can explore the API using the [playground](https://androidmakers-2023.ew.r.appspot.com/playground):

```
https://androidmakers-2023.ew.r.appspot.com/playground
```

![sandbox](https://user-images.githubusercontent.com/3974977/229364452-21c8e97e-fed1-47b0-8a41-d679884e579d.png)

For an example, the following query (used in the screenshot above) returns the 100 first sessions:

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

You can modify data in [backend/service-graphql/src/main/resources/data.json](backend/service-graphql/src/main/resources/data.json). Changes are deployed when pushed to main

### Running locally: 

To run locally (requires Google Cloud service account key): 

```
./gradlew bootRun
```