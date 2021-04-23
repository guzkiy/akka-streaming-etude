# Take Home project

## General notes

1. In the general spirit of Linux / small utilities the admin tool is designed to handle big datasets and let a user pipe the output for further processing. That is achieved by streaming Debts data and processing/enriching Debt objects with extra fields "on the fly". 
2. The project is implemented in Scala utilizing akka-http framework. The choice is somewhat opinionated, but in my defence Scala does not have a good-blueprint http-client implementation, and Akka-streams provides an extensible stream-based layer to configure an http/rest client.  
3. Due to the precise nature of the money calculations BigDecimal arithmetic is used in the project.
4. The requirements to keep all the input fields in the output have been honored by the way of parsing / extending Json coming out of TrueAccord REST api. https://github.com/guzkiy/akka-streaming-etude/blob/21f973b7c93e00e822ad277b2bd9c9208f9d3596/src/main/scala/com/trueaccord/assignment/DebtBusinessRules.scala#L126

Below is a short description of the classes and interfaces.
1. PaymentsClient[T]: the trait defines a simple interface to a rest(or another) service. The interface is not strictly necessary for the implementation but simplifies the development and makes easy mocking the backend services in testing. https://github.com/guzkiy/akka-streaming-etude/blob/21f973b7c93e00e822ad277b2bd9c9208f9d3596/src/main/scala/com/trueaccord/assignment/PaymentsClient.scala#L18
2. PaymentsRestClient[T]: class with REST client implementation of (1) https://github.com/guzkiy/akka-streaming-etude/blob/21f973b7c93e00e822ad277b2bd9c9208f9d3596/src/main/scala/com/trueaccord/assignment/PaymentsClient.scala#L26
3. PaymentsRestClient: companion object for PaymentsRestClient. Most declarations there belong to configuration file but have been left in the code due to time constraints. 
4. PaymentsFacade: provides a facade interface for all TrueAccord services. The interface is not strictly necessary for the implementation but simplifies testing and mocking services.
5. PaymentsFacadeRest: the REST client that implements PaymentFacade. https://github.com/guzkiy/akka-streaming-etude/blob/21f973b7c93e00e822ad277b2bd9c9208f9d3596/src/main/scala/com/trueaccord/assignment/PaymentsFacade.scala#L27
6. DebtBusinessRules: business functions implementations https://github.com/guzkiy/akka-streaming-etude/blob/21f973b7c93e00e822ad277b2bd9c9208f9d3596/src/main/scala/com/trueaccord/assignment/DebtBusinessRules.scala#L13
7. DebtTransformations: rest-calls, logic for enriching the Debt object with calculated fields, Json transformations https://github.com/guzkiy/akka-streaming-etude/blob/21f973b7c93e00e822ad277b2bd9c9208f9d3596/src/main/scala/com/trueaccord/assignment/DebtBusinessRules.scala#L116
8. AdminTool: the main class for the utility

## Testing strategy 
Testing coverage is built on ScalaTest and ScalaMock frameworks. There are two main test suites as follows.
1. DebtBusinessRulesTestSuite: is intended to cover the Debt functions/rules in the DebtBusinessRules. https://github.com/guzkiy/akka-streaming-etude/blob/21f973b7c93e00e822ad277b2bd9c9208f9d3596/src/test/scala/com/trueaccord/assignment/tests/DebtBusinessRulesTestSuite.scala#L8
2. DebtTransformationsTestSuite: is intended to cover the web-calls and enrichment logic by mocking the PaymentFacade for testing purposes. https://github.com/guzkiy/akka-streaming-etude/blob/21f973b7c93e00e822ad277b2bd9c9208f9d3596/src/test/scala/com/trueaccord/assignment/tests/DebtTransformationsTestSuite.scala#L15
## Building and running instructions
The project is build/run with standard SBT in the project directory as follows:
~~~
sbt package
sbt run
~~~
Tests can be run as follows
~~~
sbt test
~~~
The expected output is as follows:
~~~
$ sbt test
[info] welcome to sbt 1.4.7 (Ubuntu Java 11.0.10)
[info] DebtBusinessRulesTestSuite:
[info] - is_in_payment_plan tests
[info] - is_in_payment_plan tests with no plan
[info] - is_in_payment_plan tests debt paid off
[info] - remaining amount for debts in progress
[info] - remaining amount for debts paid off
[info] - next payment due
[info] - next payment due with payments before the plan start date
[info] - next payment due with no plan
[info] - next payment due with paid off debt
[info] DebtTransformationsTestSuite:
[info] - enrich Debt with a payment plan
[info] Run completed in 404 milliseconds.
[info] Total number of tests run: 10
[info] Suites: completed 2, aborted 0
[info] Tests: succeeded 10, failed 0, canceled 0, ignored 0, pending 0
[info] All tests passed.
[success] Total time: 4 s, completed Apr 22, 2021, 7:23:07 PM

~~~
## Directions for improvements
1. Error handling. Error handling at the moment is minimalistic and can be improved. 
2. Test coverage. Test coverage can be extended further for the Rest client implementation (PaymentsRestClient) by mocking a test rest-server in the tests. Typically it is implemented to test connection errors handling and I felt it could be outside of the limits of this project. 
3. Logging. 

## Extensibility 
1. Data model extensibility. The data model extensibility is typically a balance between flexibility of the format (Json in this case) and convenience of developing and testing the code. I tried to leave the room for the data model extensibility by being Json extra elements agnostic (so the extra fields can be added to the Debt object) and taking advantage of compiler controlled case classes for the fields needed in the calculation logic.
2. Code extensibility. 
   - The Debt API services are called via interfaces (that helps with mocking them for testing as well), and can be implemented in different protocol (binary?). The best way to achieve that would be a factory object for services facade but i did not try to implement that much as felt the time constraints. 
   - Stream processing logic is build in functional style using Akka streaming primitives (Flow, Sink etc), and can be extended/different implementation can be supplied into the processing flow.
    
3. Parallel computations. Scalability can be achieved by processing Debts in a multithreaded mode with minimalistic turning of the Akka pipeline ( runAsync(NThreads)(Sink...))

