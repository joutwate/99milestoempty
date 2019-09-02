# 99milestoempty
Code for the various tutorials hosted on https://99milestoempty.com.

###01-springboot-ecommerce
For lack of a better name, this tutorial covers building the basic necessities for a web site using Spring. This includes the ability to register and verify new users, authenticate users with multi-factor authentication, authorize varying levels of requests utilizing JWT tokens, sending emails, data encryption and accepting payments.

The goal of this tutorial is to show a basic implementation of these features. In future tutorials we will look into migrating some of the features to more professional level services like those provided by Amazon.

#### How to build and run
`mvn spring-boot:run`


#### Logging in and testing endpoints

To test out our endpoints and verify that they are only accessible when access is authorized we can use an app like Postman to test it out.
First we would call the login endpoint that would normally be called by an HTML form to get our JWT. The call below will login as a basic user.
```
POST https://localhost:8443/api/auth/signin
Content-Type: application/x-www-form-urlencoded
 
username=user&password=password
``` 

This call will return a JWT that we will use in subsequent requests. 
```
HTTP/1.1 200 
X-Content-Type-Options: nosniff
X-XSS-Protection: 1; mode=block
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Pragma: no-cache
Expires: 0
Strict-Transport-Security: max-age=31536000 ; includeSubDomains
X-Frame-Options: DENY
Content-Type: application/json;charset=UTF-8
Transfer-Encoding: chunked
Date: Sat, 31 Aug 2019 03:19:43 GMT

{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhZG1pbiIsImlhdCI6MTU2NzIyMTU4MywiZXhwIjoxNTY3MzA3OTgzfQ.66cVDG96aZyH6BKSKfEIju20MHwAG4v2ElvkhgMVgYJbM5uiR1TVPfoJ6tqJfRsAk-kRvj644vNwc4vOtyOyvA",
  "tokenType": "Bearer"
}

Response code: 200; Time: 674ms; Content length: 213 bytes
```
The first one we'll test is /user and see that the call is made successfully when we add the JWT to our request headers. 

```
GET https://localhost:8443/user
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ1c2VyIiwiaWF0IjoxNTY3MzA3NDI0LCJleHAiOjE1NjczOTM4MjR9.zFuQv6wTfO6rB0xpS0vNAJvkFzSHHSYPBCXHj-5l4V38mRfYorQ0W11HItWJNFfScznMOOiakkfZwNopeTPB9g
```

```
HTTP/1.1 200 
X-Content-Type-Options: nosniff
X-XSS-Protection: 1; mode=block
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Pragma: no-cache
Expires: 0
Strict-Transport-Security: max-age=31536000 ; includeSubDomains
X-Frame-Options: DENY
Content-Type: text/plain;charset=UTF-8
Content-Length: 11
Date: Sun, 01 Sep 2019 03:11:11 GMT

Hello user!

Response code: 200; Time: 119ms; Content length: 11 bytes
```

Now let's make a call to the /admin endpoint with the basic user and see that they will be denied access due to privileges.
```
GET https://localhost:8443/admin
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ1c2VyIiwiaWF0IjoxNTY3MzA3NDI0LCJleHAiOjE1NjczOTM4MjR9.zFuQv6wTfO6rB0xpS0vNAJvkFzSHHSYPBCXHj-5l4V38mRfYorQ0W11HItWJNFfScznMOOiakkfZwNopeTPB9g
```

```
HTTP/1.1 403 
X-Content-Type-Options: nosniff
X-XSS-Protection: 1; mode=block
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Pragma: no-cache
Expires: 0
Strict-Transport-Security: max-age=31536000 ; includeSubDomains
X-Frame-Options: DENY
Content-Type: application/json;charset=UTF-8
Transfer-Encoding: chunked
Date: Sun, 01 Sep 2019 03:13:20 GMT

{
  "timestamp": "2019-09-01T03:13:20.714+0000",
  "status": 403,
  "error": "Forbidden",
  "message": "Forbidden",
  "path": "/admin"
}

Response code: 403; Time: 93ms; Content length: 115 bytes
```