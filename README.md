# Take Home project

## General notes
In the general spirit of Linux / small utilities the admin tool is designed to handle big datasets and pipe the output into something else. That is achieved by handling streaming volumes of Debts data and processing/enriching Debt objects with extra fields in the stream. The requirements to keep all the input fields in the output of the Debt objects has been honored by the way of parsing / extending Json coming out of TrueAccord REST api.
The project is implemented in Scala utilizing akka-http framework. The choice is somewhat opinionated, but in my defence Scala does not have a good-blueprint http-client implementation, and Akka-streams provides an extensible stream-based layer to configure an http/rest client.  
Below is a short description of the classes and interfaces.
1. PaymentsClient[T]: the trait defines a simple interface to a rest(or another) service. The interface is not strictly necessary for the implementation but simplifies the development and makes easy mocking the backend services in testing.
2. PaymentsRestClient[T]: class with REST client implementation of (1) 
3. PaymentsRestClient: companion object for PaymentsRestClient. Most declarations there belong to configuration file but have been left in the code due to time constraints. 
4. PaymentsFacade: provides a facade interface for all TrueAccord services. The interface is not strictly necessary for the implementation but simplifies testing and mocking services.
5. PaymentsFacadeRest: the REST client that implements PaymentFacade. 



## Testing strategy 

## Building instructions
## Direction for improvements
1. error handling 
2. Test coverage
## Extensibility 