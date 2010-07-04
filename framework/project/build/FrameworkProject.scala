import sbt._
import sbt.ScalaProject

trait IgnorePOM extends BasicManagedProject
{
    val scalaToolsSnapshots = ScalaToolsSnapshots
    val scalatest = "org.scalatest" % "scalatest" % "1.2-for-scala-2.8.0.RC6-SNAPSHOT"
}

class FrameworkProject(info: ProjectInfo) extends ParentProject(info) with IgnorePOM
{
    val mavenLocal  = "Local Maven Repository" at "file://"+Path.userHome+"/.m2/repository"

    lazy val liftBase        = project ("lift-base",        "lift-base",        new LiftBaseProject(_))
    lazy val liftModules     = project ("lift-modules",     "lift-modules",     new LiftModulesProject(_))
    lazy val liftPersistence = project ("lift-persistence", "lift-persistence", new LiftPersistenceProject(_))

    class LiftPersistenceProject(info: ProjectInfo) extends ParentProject(info) with IgnorePOM
    {
        lazy val liftWebkit  = (liftBase.asInstanceOf[LiftBaseProject].liftWebkit)
        lazy val liftUtil    = (liftBase.asInstanceOf[LiftBaseProject].liftUtil)
        lazy val liftJson    = (liftBase.asInstanceOf[LiftBaseProject].liftJson)
        lazy val liftCommon  = (liftBase.asInstanceOf[LiftBaseProject].liftCommon)

        lazy val liftMapper  = project ("lift-mapper", "lift-mapper", new LiftMapperProject(_), liftWebkit) 
        lazy val liftRecord  = project ("lift-record", "lift-record", new LiftRecordProject(_), liftMapper) 
        lazy val liftJPA     = project ("lift-jpa",    "lift-jpa",    new LiftJPAProject(_),    liftWebkit) 

        lazy val liftCouchDB = project ("lift-couchdb", "lift-couchdb", new LiftCouchDBProject(_), liftRecord) 
        lazy val liftMongoDB = project ("lift-mongodb", "lift-mongodb", new LiftMongoDBProject(_), liftJson, liftCommon) 
        lazy val liftMongoDBRecord = project ("lift-mongodb-record", "lift-mongodb-record", 
                                              new LiftMongoDBRecordProject(_), liftJson, liftRecord, liftMongoDB) 

        class LiftMapperProject (info: ProjectInfo) extends DefaultProject(info) with IgnorePOM
        class LiftRecordProject (info: ProjectInfo) extends DefaultProject(info) with IgnorePOM
        class LiftCouchDBProject (info: ProjectInfo) extends DefaultProject(info) with IgnorePOM 
        {
            val t1 = "net.databinder" % "dispatch-http_2.8.0.RC6" % "0.7.4"
            val t2 = "net.jcip" % "jcip-annotations" % "1.0"
        }

        class LiftJPAProject (info: ProjectInfo) extends DefaultProject(info) with IgnorePOM
        {
            val t1 = "org.scala-libs" % "scalajpa" % "1.2-scala280-SNAPSHOT"
            val t2 = "javax.persistence" % "persistence-api" % "1.0"
        }

        class LiftMongoDBProject (info: ProjectInfo) extends DefaultProject(info) with IgnorePOM
        {
            val t1 = "org.mongodb" % "mongo-java-driver" % "2.0"
        }

        class LiftMongoDBRecordProject (info: ProjectInfo) extends DefaultProject(info) with IgnorePOM
        {
            val t1 = "org.mongodb" % "mongo-java-driver" % "2.0"
        }

    }

    class LiftModulesProject(info: ProjectInfo) extends ParentProject(info) with IgnorePOM
    {
        lazy val liftWebkit  = (liftBase.asInstanceOf[LiftBaseProject].liftWebkit)
        lazy val liftActor   = (liftBase.asInstanceOf[LiftBaseProject].liftActor)
        lazy val liftUtil    = (liftBase.asInstanceOf[LiftBaseProject].liftUtil)
        lazy val liftMapper  = (liftPersistence.asInstanceOf[LiftPersistenceProject].liftMapper)

        lazy val liftWidgets  = project ("lift-widgets",  "lift-widgets",  new LiftWidgetsProject(_),  liftWebkit) 
        lazy val liftAmqp     = project ("lift-amqp",     "lift-amqp",     new LiftAmqpProject(_),     liftActor) 
        lazy val liftFacebook = project ("lift-facebook", "lift-facebook", new LiftFacebookProject(_), liftWebkit)
        lazy val liftImaging  = project ("lift-imaging",  "lift-imaging",  new LiftImagingProject(_),  liftUtil)
        lazy val liftJTA      = project ("lift-jta",      "lift-jta",      new LiftJTAProject(_),      liftUtil)
        lazy val liftLDAP     = project ("lift-ldap",     "lift-ldap",     new LiftLDAPProject(_),     liftMapper)
        lazy val liftMachine  = project ("lift-machine",  "lift-machine",  new LiftMachineProject(_),  liftMapper)
        lazy val liftOAuth    = project ("lift-oauth",    "lift-oauth",    new LiftOAuthProject(_),    liftWebkit)
        lazy val liftOpenID   = project ("lift-openid",   "lift-openid",   new LiftOpenIDProject(_),   liftMapper)
        //lazy val liftOSGi     = project ("lift-osgi",     "lift-osgi",     new LiftOSGiProject(_),     liftWebkit)
        lazy val liftPaypal   = project ("lift-paypal",   "lift-paypal",   new LiftPaypalProject(_),   liftWebkit)
        lazy val liftScalate  = project ("lift-scalate",  "lift-scalate",  new LiftScalateProject(_),  liftWebkit)
        lazy val liftTestkit  = project ("lift-testkit",  "lift-testkit",  new LiftTestkitProject(_),  liftUtil)
        lazy val liftTextile  = project ("lift-textile",  "lift-textile",  new LiftTextileProject(_),  liftUtil)
        lazy val liftWizard   = project ("lift-wizard",   "lift-wizard",   new LiftWizardProject(_),   liftMapper)
        lazy val liftXMPP     = project ("lift-xmpp",     "lift-xmpp",     new LiftXMPPProject(_),     liftActor)

        lazy val liftOAuthMapper = project ("lift-oauth-mapper", "lift-oauth-mapper",
                                            new LiftOAuthMapperProject(_), liftOAuth, liftMapper)

        class LiftWidgetsProject     (info: ProjectInfo) extends DefaultProject(info) with IgnorePOM
        class LiftFacebookProject    (info: ProjectInfo) extends DefaultProject(info) with IgnorePOM
        class LiftLDAPProject        (info: ProjectInfo) extends DefaultProject(info) with IgnorePOM
        class LiftMachineProject     (info: ProjectInfo) extends DefaultProject(info) with IgnorePOM
        class LiftOAuthProject       (info: ProjectInfo) extends DefaultProject(info) with IgnorePOM
        class LiftOAuthMapperProject (info: ProjectInfo) extends DefaultProject(info) with IgnorePOM
        class LiftTextileProject     (info: ProjectInfo) extends DefaultProject(info) with IgnorePOM
        class LiftWizardProject      (info: ProjectInfo) extends DefaultProject(info) with IgnorePOM

        class LiftXMPPProject (info: ProjectInfo) extends DefaultProject(info) with IgnorePOM
        {
            val t1 = "jivesoftware" % "smack" % "3.1.0"
            val t2 = "jivesoftware" % "smackx" % "3.1.0"
        }

        class LiftTestkitProject (info: ProjectInfo) extends DefaultProject(info) with IgnorePOM
        {
            val t1 = "javax.servlet" % "servlet-api" % "2.5"
            val t2 = "commons-httpclient" % "commons-httpclient" % "3.1"
        }

        class LiftScalateProject (info: ProjectInfo) extends DefaultProject(info) with IgnorePOM
        {
            val t1 = "javax.servlet" % "servlet-api" % "2.5"
            val t2 = "org.fusesource.scalate" % "scalate-core" % "1.1"
        }

        class LiftPaypalProject (info: ProjectInfo) extends DefaultProject(info) with IgnorePOM
        {
            val t1 = "commons-httpclient" % "commons-httpclient" % "3.1"
        }

        class LiftOpenIDProject (info: ProjectInfo) extends DefaultProject(info) with IgnorePOM
        {
            val t1 = "org.openid4java" % "openid4java-consumer" % "0.9.5"
        }

        class LiftOSGiProject (info: ProjectInfo) extends DefaultProject(info) with IgnorePOM
        {
            val t1 = "org.scalamodules" % "scalamodules-core" % "1.5"
            val t2 = "org.apache.felix" % "org.osgi.compendium" % "1.4.0"
            val t3 = "org.ops4j.pax.web" % "pax-web-api" % "0.7.2"
            val t4 = "org.ops4j.pax.swissbox" % "pax-swissbox-extender" % "1.2.0"
            val t5 = "javax.servlet" % "servlet-api" % "2.5"
        }

        class LiftAmqpProject (info: ProjectInfo) extends DefaultProject(info) with IgnorePOM
        {
            val t1 = "com.rabbitmq" % "amqp-client" % "1.7.2"
        }

        class LiftImagingProject (info: ProjectInfo) extends DefaultProject(info) with IgnorePOM
        {
            val t1 = "org.apache.sanselan" % "sanselan" % "0.97-incubator"
        }

        class LiftJTAProject (info: ProjectInfo) extends DefaultProject(info) with IgnorePOM
        {
            val t1 = "org.scala-libs" % "scalajpa"         % "1.2-scala280-SNAPSHOT"
            val t2 = "com.atomikos"   % "transactions"     % "3.2.3"
            val t3 = "com.atomikos"   % "transactions-jta" % "3.2.3"
            val t4 = "com.atomikos"   % "transactions-api" % "3.2.3"
            val t5 = "com.atomikos"   % "atomikos-util"    % "3.2.3"

            val t6 = "org.hibernate"  % "hibernate-entitymanager" % "3.4.0.GA"
            val t7 = "javax.persistence"         % "persistence-api"       % "1.0"
            val t8 = "org.apache.geronimo.specs" % "geronimo-jta_1.1_spec" % "1.1.1"

        }

    }


    class LiftBaseProject(info: ProjectInfo) extends ParentProject(info) with IgnorePOM
    {
        lazy val liftCommon = project ("lift-common", "lift-common", new LiftCommonProject(_))
        lazy val liftActor  = project ("lift-actor", "lift-actor", new LiftActorProject(_), liftCommon)
        lazy val liftJson   = project ("lift-json", "lift-json", new LiftJsonProject(_))
        lazy val liftUtil   = project ("lift-util", "lift-util", new LiftUtilProject(_), liftActor, liftJson)
        lazy val liftWebkit = project ("lift-webkit", "lift-webkit", new LiftWebkitProject(_), liftUtil, liftJson)

        class LiftActorProject (info: ProjectInfo) extends DefaultProject(info) with IgnorePOM

        class LiftWebkitProject (info: ProjectInfo) extends DefaultProject(info)
        {
            val t1 = "commons-fileupload" % "commons-fileupload" % "1.2.1"
            val t2 = "javax.servlet" % "servlet-api" % "2.5"
        }
    
        class LiftUtilProject (info: ProjectInfo) extends DefaultProject(info)
        {
            val t1 = "javax.mail" % "mail" % "1.4.1"
            val t2 = "commons-codec" % "commons-codec" % "1.3"
            val t3 = "org.slf4j" % "slf4j-api" % "1.5.11"
            val t4 = "joda-time" % "joda-time" % "1.6"
            val t5 = "org.slf4j" % "slf4j-log4j12" % "1.5.11"
        }
    
        class LiftJsonProject (info: ProjectInfo) extends DefaultProject(info)
        {
            val t1 = "com.thoughtworks.paranamer" % "paranamer" % "2.0"
        }
    
        class LiftCommonProject (info: ProjectInfo) extends DefaultProject(info)
        {
            val t1 = "org.slf4j" % "slf4j-api" % "1.5.11"
            val t2 = "ch.qos.logback" % "logback-classic" % "0.9.20"
            val t3 = "ch.qos.logback" % "logback-classic" % "0.9.20"
            val t4 = "log4j" % "log4j" % "1.2.14"
        }

    }
}

