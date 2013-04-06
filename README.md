# j-javaactors-ibm

###### `This repo is only for reference from sourcecode published in ibm-developerworks on 2012.05.30`

A Java actor library for parallel execution. Modernize common concurrency patterns with JavaActors, a lightweight Java actor library.

JavaActors is built around three core interfaces:

* **Message** is a message sent between actors. Message is a container for three (optional) values and some behavior:
  * source is the sending actor.
  * subject is a string defining the meaning of the message (also known as a command).
  * data is any parameter data for the message; often a map, list, or array. Parameters can be data to process and/or other actors to interact with.
  * subjectMatches() checks to see if the message subject matches a string or regular expression.

  The default message class for the JavaActors package is *DefaultMessage*.
* **ActorManager** is a manager of actors. It is responsible for allocating threads (and thus processors) to actors to process messages. ActorManager has the following key behaviors or characteristics:
  * createActor() creates an actor and associates it with this manager.
  * startActor() starts an actor.
  * detachActor() stops an actor and disassociates it from this manager.
  * send()/broadcast() sends a message to an actor, a set of actors, any actor for a category, or all actors.

  In most programs, there is a single ActorManager, although multiple are allowed if you want to manage multiple thread and/or actor pools. The default implementation of this interface is *DefaultActorManager*.
* **Actor** is a unit of execution that processes messages one at a time. Actors have the following key behaviors or characteristics:
  * Each actor has a name, which must be unique per ActorManager.
  * Each actor belongs to a category; categories are a means to send messages to one member of a group of actors. An actor can belong to only one category at a time.
  * receive() is called whenever the ActorManager can provide a thread to execute the actor on. It is called only when a message for the actor exists. To be most effective, an actor should process messages quickly and not enter long waits (such as for human input).
  * willReceive() allows the actor to filter potential message subjects.
  * peek() allows the actor and others to see if there are pending messages, possibly for select subjects.
  * remove() allows the actor and others to remove or cancel any yet unprocessed messages.
  * getMessageCount() allows the actor and others to get the number of pending messages.
  * getMaxMessageCount() allows the actor to limit how many pending messages are supported; this method can be used to prevent runaway sends.

  Most programs have many actors, often of different types. Actors can be created at the start of a program or created (and destroyed) as a program executes. The actor package in this article includes an abstract class called *AbstractActor*, on which actor implementations are based. 

[Read the full article...](http://www.ibm.com/developerworks/java/library/j-javaactors/) 

---

> **Original Author:**

> Barry Feigenbaum, software engineer currently *(30 May 2012)* working at Dell and previously at IBM and Amazon. He is a Sun (now Oracle) Certified Java Programmer, Developer and Architect. Barry has authored several other developerWorks articles and presented at conferences such as JavaOne, as well as authoring several technical books. He holds a Ph.D. in Computer Engineering.

> **Original Source:**

> http://www.ibm.com/developerworks/java/library/j-javaactors/


> **Original License:** 

> The following are terms of a legal downloader agreement (the "Agreement") regarding Your download of Content (as defined below) from this Website. IBM may change these terms of use and other requirements and guidelines for use of this Website at its sole discretion. This Website may contain other proprietary notices and copyright information the terms of which must be observed and followed. Any use of the Content in violation of this Agreement is strictly prohibited.

> "Content" includes, but is not limited to, software, text and/or speech files, code, associated materials, media and /or documentation that You download from this Website. The Content may be provided by IBM or third-parties. IBM Content is owned by IBM and is copyrighted and licensed, not sold. Third-party Content is owned by its respective owner and is licensed by the third party directly to You. IBM's decision to permit posting of third-party Content does not constitute an express or implied license from IBM to You or a recommendation or endorsement by IBM of any particular product, service, company or technology.

> The party providing the Content (the "Provider") grants You a nonexclusive, worldwide, irrevocable, royalty-free, copyright license to edit, copy, reproduce, publish, publicly display and/or perform, format, modify and/or make derivative works of, translate, re-arrange, and distribute the Content or any portions thereof and to sublicense any or all such rights and to permit sublicensees to further sublicense such rights, for both commercial and non-commercial use, provided You abide by the terms of this Agreement. You understand that no assurances are provided that the Content does not infringe the intellectual property rights of any other entity. Neither IBM nor the provider of the Content grants a patent license of any kind, whether expressed or implied or by estoppel. As a condition of exercising the rights and licenses granted under this Agreement, You assume sole responsibility to obtain any other intellectual property rights needed.

> The Provider of the Content is the party that submitted the Content for Posting and who represents and warrants that they own all of the Content, (or have obtained all written releases, authorizations and licenses from any other owner(s) necessary to grant IBM and downloaders this license with respect to portions of the Content not owned by the Provider). All information provided on or through this Website may be changed or updated without notice. You understand that IBM has no obligation to check information and /or Content on the Website and that the information and/or Content provided on this Web site may contain technical inaccuracies or typographical errors.

> IBM may, in its sole discretion, discontinue the Website, any service provided on or through the Website, as well as limit or discontinue access to any Content posted on the Website for any reason without notice. IBM may terminate this Agreement and Your rights to access, use and download Content from the Website at any time, with or without cause, immediately and without notice.

> ALL INFORMATION AND CONTENT IS PROVIDED ON AN "AS IS" BASIS. IBM MAKES NO REPRESENTATIONS OR WARRANTIES, EXPRESS OR IMPLIED, CONCERNING USE OF THE WEBSITE, THE CONTENT, OR THE COMPLETENESS OR ACCURACY OF THE CONTENT OR INFORMATION OBTAINED FROM THE WEBSITE. IBM SPECIFICALLY DISCLAIMS ALL WARRANTIES WITH REGARD TO THE IMPLIED WARRANTIES OF NON-INFRINGEMENT, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. IBM DOES NOT WARRANT UNINTERRUPTED OR ERROR-FREE OPERATION OF ANY CONTENT. IBM IS NOT RESPONSIBLE FOR THE RESULTS OBTAINED FROM THE USE OF THE CONTENT OR INFORMATION OBTAINED FROM THE WEBSITE.

> LIMITATION OF LIABILITY. IN NO EVENT WILL IBM BE LIABLE TO ANY PARTY FOR ANY DIRECT, INDIRECT, SPECIAL OR OTHER CONSEQUENTIAL DAMAGES FOR ANY USE OF THIS WEBSITE, THE USE OF CONTENT FROM THIS WEBSITE, OR ON ANY OTHER HYPER LINKED WEB SITE, INCLUDING, WITHOUT LIMITATION, ANY LOST PROFITS, BUSINESS INTERRUPTION, LOSS OF PROGRAMS OR OTHER DATA ON YOUR INFORMATION HANDLING SYSTEM OR OTHERWISE, EVEN IF IBM IS EXPRESSLY ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.

> The laws of the State of New York, USA govern this Agreement, without reference to conflict of law principles. The "United Nations Convention on International Sale of Goods" does not apply. This Agreement may not be assigned by You. The parties agree to waive their right to a trial by jury.

> This Agreement is the complete and exclusive agreement between the parties and supersedes all prior agreements, oral or written, and all other communications relating to the subject matter hereof. For clarification, it is understood and You agree, that any additional agreement or license terms that may accompany the Content is invalid, void, and non-enforceable to any downloader of this Content including IBM.

> If any section of this Agreement is found by competent authority to be invalid, illegal or unenforceable in any respect for any reason, the validity, legality and enforceability of any such section in every other respect and the remainder of this Agreement shall continue in effect. 

