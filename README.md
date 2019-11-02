[![GitHub license](https://img.shields.io/github/license/mashape/apistatus.svg)](https://github.com/galleog/piggymetrics-k8s/blob/k8s/LICENCE)
[![Build Status](https://travis-ci.org/galleog/piggymetrics-k8s.svg?branch=master)](https://travis-ci.org/galleog/piggymetrics-k8s)

# Piggy Metrics

This project demonstrates [Microservice Architecture Pattern](http://martinfowler.com/microservices/) using Spring Boot, Spring Cloud, Kubernetes, Istio and gRPC.
It is derived from the MIT-licensed code of Alexander Lukyanchikov, [PiggyMetrics](https://github.com/sqshq/PiggyMetrics).

![](https://cloud.githubusercontent.com/assets/6069066/13864234/442d6faa-ecb9-11e5-9929-34a9539acde0.png)
![Piggy Metrics](https://cloud.githubusercontent.com/assets/6069066/13830155/572e7552-ebe4-11e5-918f-637a49dff9a2.gif)

## Functional services

PiggyMetrics was decomposed into three core microservices. All of them are independently deployable applications, organized around certain business domains.

<img width="880" alt="Functional services" src="https://cloud.githubusercontent.com/assets/6069066/13900465/730f2922-ee20-11e5-8df0-e7b51c668847.png">

#### Account service
Contains general user input logic and validation: incomes/expenses items, savings and account settings.

Method	| Path	| Description	| User authenticated	| Available from UI
------------- | ------------------------- | ------------- |:-------------:|:----------------:|
GET	| /accounts/{account}	| Get specified account data	|  | 	
GET	| /accounts/current	| Get current account data	| × | ×
GET	| /accounts/demo	| Get demo account data (pre-filled incomes/expenses items, etc)	|   | 	×
PUT	| /accounts/current	| Save current account data	| × | ×
POST	| /accounts/	| Register new account	|   | ×


#### Statistics service
Performs calculations on major statistics parameters and captures time series for each account. Datapoint contains values, normalized to base currency and time period. This data is used to track cash flow dynamics in account lifetime.

Method	| Path	| Description	| User authenticated	| Available from UI
------------- | ------------------------- | ------------- |:-------------:|:----------------:|
GET	| /statistics/{account}	| Get statistics for the specifed account	          |  | 	
GET	| /statistics/current	| Get statistics for the current account	| × | × 
GET	| /statistics/demo	| Get demo account statistics	|   | × 
PUT	| /statistics/{account}	| Create or update time series datapoint for the specified account	|   | 


#### Notification service
Stores users contact information and notification settings (like remind and backup frequency). Scheduled worker collects required information from other services and sends e-mail messages to subscribed customers.

Method	| Path	| Description	| User authenticated	| Available from UI
------------- | ------------------------- | ------------- |:-------------:|:----------------:|
GET	| /notifications/settings/current	| Get current account notification settings	| × | ×	
PUT	| /notifications/settings/current	| Save current account notification settings	| × | ×

#### Notes
- Each microservice has it's own database, so there is no way to bypass API and access persistance data directly.
- In this project, I use MongoDB as a primary database for each service. It might also make sense to have a polyglot persistence architecture (сhoose the type of db that is best suited to service requirements).
- Service-to-service communication is quite simplified: microservices talking using only synchronous REST API. Common practice in a real-world systems is to use combination of interaction styles. For example, perform synchronous GET request to retrieve data and use asynchronous approach via Message broker for create/update operations in order to decouple services and buffer messages. However, this brings us to the [eventual consistency](http://martinfowler.com/articles/microservice-trade-offs.html#consistency) world.

## Infrastructure services
There's a bunch of common patterns in distributed systems, which could help us to make described core services work. [Spring cloud](http://projects.spring.io/spring-cloud/) provides powerful tools that enhance Spring Boot applications behaviour to implement those patterns. I'll cover them briefly.
<img width="880" alt="Infrastructure services" src="https://cloud.githubusercontent.com/assets/6069066/13906840/365c0d94-eefa-11e5-90ad-9d74804ca412.png">
### Config service
[Spring Cloud Config](http://cloud.spring.io/spring-cloud-config/spring-cloud-config.html) is horizontally scalable centralized configuration service for distributed systems. It uses a pluggable repository layer that currently supports local storage, Git, and Subversion. 

In this project, I use `native profile`, which simply loads config files from the local classpath. You can see `shared` directory in [Config service resources](https://github.com/sqshq/PiggyMetrics/tree/master/config/src/main/resources). Now, when Notification-service requests it's configuration, Config service responses with `shared/notification-service.yml` and `shared/application.yml` (which is shared between all client applications).

##### Client side usage
Just build Spring Boot application with `spring-cloud-starter-config` dependency, autoconfiguration will do the rest.

Now you don't need any embedded properties in your application. Just provide `bootstrap.yml` with application name and Config service url:
```yml
spring:
  application:
    name: notification-service
  cloud:
    config:
      uri: http://config:8888
      fail-fast: true
```

##### With Spring Cloud Config, you can change app configuration dynamically. 
For example, [EmailService bean](https://github.com/sqshq/PiggyMetrics/blob/master/notification-service/src/main/java/com/piggymetrics/notification/service/EmailServiceImpl.java) was annotated with `@RefreshScope`. That means, you can change e-mail text and subject without rebuild and restart Notification service application.

First, change required properties in Config server. Then, perform refresh request to Notification service:
`curl -H "Authorization: Bearer #token#" -XPOST http://127.0.0.1:8000/notifications/refresh`

Also, you could use Repository [webhooks to automate this process](http://cloud.spring.io/spring-cloud-config/spring-cloud-config.html#_push_notifications_and_spring_cloud_bus)

##### Notes
- There are some limitations for dynamic refresh though. `@RefreshScope` doesn't work with `@Configuration` classes and doesn't affect `@Scheduled` methods
- `fail-fast` property means that Spring Boot application will fail startup immediately, if it cannot connect to the Config Service.
- There are significant [security notes](https://github.com/sqshq/PiggyMetrics#security) below

### Auth service
Authorization responsibilities are completely extracted to separate server, which grants [OAuth2 tokens](https://tools.ietf.org/html/rfc6749) for the backend resource services. Auth Server is used for user authorization as well as for secure machine-to-machine communication inside a perimeter.

In this project, I use [`Password credentials`](https://tools.ietf.org/html/rfc6749#section-4.3) grant type for users authorization (since it's used only by native PiggyMetrics UI) and [`Client Credentials`](https://tools.ietf.org/html/rfc6749#section-4.4) grant for microservices authorization.

Spring Cloud Security provides convenient annotations and autoconfiguration to make this really easy to implement from both server and client side. You can learn more about it in [documentation](http://cloud.spring.io/spring-cloud-security/spring-cloud-security.html) and check configuration details in [Auth Server code](https://github.com/sqshq/PiggyMetrics/tree/master/auth-service/src/main/java/com/piggymetrics/auth).

From the client side, everything works exactly the same as with traditional session-based authorization. You can retrieve `Principal` object from request, check user's roles and other stuff with expression-based access control and `@PreAuthorize` annotation.

Each client in PiggyMetrics (account-service, statistics-service, notification-service and browser) has a scope: `server` for backend services, and `ui` - for the browser. So we can also protect controllers from external access, for example:

``` java
@PreAuthorize("#oauth2.hasScope('server')")
@RequestMapping(value = "accounts/{name}", method = RequestMethod.GET)
public List<DataPoint> getStatisticsByAccountName(@PathVariable String name) {
	return statisticsService.findByAccountName(name);
}
```

### API Gateway
As you can see, there are three core services, which expose external API to client. In a real-world systems, this number can grow very quickly as well as whole system complexity. Actually, hundreds of services might be involved in rendering of one complex webpage.

In theory, a client could make requests to each of the microservices directly. But obviously, there are challenges and limitations with this option, like necessity to know all endpoints addresses, perform http request for each peace of information separately, merge the result on a client side. Another problem is non web-friendly protocols which might be used on the backend.

Usually a much better approach is to use API Gateway. It is a single entry point into the system, used to handle requests by routing them to the appropriate backend service or by invoking multiple backend services and [aggregating the results](http://techblog.netflix.com/2013/01/optimizing-netflix-api.html). Also, it can be used for authentication, insights, stress and canary testing, service migration, static response handling, active traffic management.

Netflix opensourced [such an edge service](http://techblog.netflix.com/2013/06/announcing-zuul-edge-service-in-cloud.html), and now with Spring Cloud we can enable it with one `@EnableZuulProxy` annotation. In this project, I use Zuul to store static content (ui application) and to route requests to appropriate microservices. Here's a simple prefix-based routing configuration for Notification service:

```yml
zuul:
  routes:
    notification-service:
        path: /notifications/**
        serviceId: notification-service
        stripPrefix: false

```

That means all requests starting with `/notifications` will be routed to Notification service. There is no hardcoded address, as you can see. Zuul uses [Service discovery](https://github.com/sqshq/PiggyMetrics/blob/master/README.md#service-discovery) mechanism to locate Notification service instances and also [Circuit Breaker and Load Balancer](https://github.com/sqshq/PiggyMetrics/blob/master/README.md#http-client-load-balancer-and-circuit-breaker), described below.

### Monitor dashboard

In this project configuration, each microservice with Hystrix on board pushes metrics to Turbine via Spring Cloud Bus (with AMQP broker). The Monitoring project is just a small Spring boot application with [Turbine](https://github.com/Netflix/Turbine) and [Hystrix Dashboard](https://github.com/Netflix/Hystrix/tree/master/hystrix-dashboard).

See below [how to get it up and running](https://github.com/sqshq/PiggyMetrics#how-to-run-all-the-things).

Let's see our system behavior under load: Account service calls Statistics service and it responses with a vary imitation delay. Response timeout threshold is set to 1 second.

<img width="880" src="https://cloud.githubusercontent.com/assets/6069066/14194375/d9a2dd80-f7be-11e5-8bcc-9a2fce753cfe.png">

<img width="212" src="https://cloud.githubusercontent.com/assets/6069066/14127349/21e90026-f628-11e5-83f1-60108cb33490.gif">	| <img width="212" src="https://cloud.githubusercontent.com/assets/6069066/14127348/21e6ed40-f628-11e5-9fa4-ed527bf35129.gif"> | <img width="212" src="https://cloud.githubusercontent.com/assets/6069066/14127346/21b9aaa6-f628-11e5-9bba-aaccab60fd69.gif"> | <img width="212" src="https://cloud.githubusercontent.com/assets/6069066/14127350/21eafe1c-f628-11e5-8ccd-a6b6873c046a.gif">
--- |--- |--- |--- |
| `0 ms delay` | `500 ms delay` | `800 ms delay` | `1100 ms delay`
| Well behaving system. The throughput is about 22 requests/second. Small number of active threads in Statistics service. The median service time is about 50 ms. | The number of active threads is growing. We can see purple number of thread-pool rejections and therefore about 30-40% of errors, but circuit is still closed. | Half-open state: the ratio of failed commands is more than 50%, the circuit breaker kicks in. After sleep window amount of time, the next request is let through. | 100 percent of the requests fail. The circuit is now permanently open. Retry after sleep time won't close circuit again, because the single request is too slow.

### Log analysis

Centralized logging can be very useful when attempting to identify problems in a distributed environment. Elasticsearch, Logstash and Kibana stack lets you search and analyze your logs, utilization and network activity data with ease.
Ready-to-go Docker configuration described [in my other project](http://github.com/sqshq/ELK-docker).

## Security

An advanced security configuration is beyond the scope of this proof-of-concept project. For a more realistic simulation of a real system, consider to use https, JCE keystore to encrypt Microservices passwords and Config server properties content (see [documentation](http://cloud.spring.io/spring-cloud-config/spring-cloud-config.html#_security) for details).

## Infrastructure automation

Deploying microservices, with their interdependence, is much more complex process than deploying monolithic application. It is important to have fully automated infrastructure. We can achieve following benefits with Continuous Delivery approach:

- The ability to release software anytime
- Any build could end up being a release
- Build artifacts once - deploy as needed

Here is a simple Continuous Delivery workflow, implemented in this project:

<img width="880" src="https://cloud.githubusercontent.com/assets/6069066/14159789/0dd7a7ce-f6e9-11e5-9fbb-a7fe0f4431e3.png">

In this [configuration](https://github.com/sqshq/PiggyMetrics/blob/master/.travis.yml), Travis CI builds tagged images for each successful git push. So, there are always `latest` image for each microservice on [Docker Hub](https://hub.docker.com/r/sqshq/) and older images, tagged with git commit hash. It's easy to deploy any of them and quickly rollback, if needed.

## How to run all the things?

Keep in mind, that you are going to start 8 Spring Boot applications, 4 MongoDB instances and RabbitMq. Make sure you have `4 Gb` RAM available on your machine. You can always run just vital services though: Gateway, Registry, Config, Auth Service and Account Service.

#### Before you start
- Install [Minikube](https://kubernetes.io/docs/tasks/tools/install-minikube/), [Helm](https://helm.sh/docs/using_helm/) and
[Istio](https://istio.io/docs/setup/kubernetes/).
- To enable automatic Istio sidecar injection label the `default` namespace with `istio-injection=enabled`:
```bash
kubectl label namespace default istio-injection=enabled
```
- Edit `istio-sidecar-injector` configMap:
```bash
kubectl edit configmap/istio-sidecar-injector -n istio-system --validate=true
```
and set its `policy` value to `disabled`. This disables automatic sidecar injection unless the pod template `spec` contains
the `sidecar.istio.io/inject` annotation with value `true`.
- Add the `bitnami` and `codecentric` Helm repositories:
```bash
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo add codecentric https://codecentric.github.io/helm-charts
```
- Clone the repository.
-
- Deploy PostgreSQL, Apache Kafka and Keycloak to Kubernetes:
```bash
helm install charts/pgm-dependencies
```

#### Production mode
In this mode, Kubernetes pods will be created using the latest images from Docker Hub. Just run
```bash
helm install charts/piggymetrics
```
Make sure all microservices runs. The command
```bash
kubectl get pods
```
should return something like:

and then go to `http://localhost`.

#### Development mode
If you'd like to build images yourself (with some changes in the code, for example), first install [Skaffold](https://skaffold.dev/).
To build and deployed all microservices to Kubernetes run `skaffold dev`.