package sampleclean

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf

import sampleclean.api.SampleCleanContext;
import sampleclean.api.SampleCleanAQP;
import sampleclean.parse.SampleCleanParser;
import sampleclean.parse.SampleCleanParser._;
import sampleclean.clean.ParametricOutlier;
import sampleclean.clean.dedup._

import org.apache.spark.sql.hive.HiveContext
import sampleclean.clean.dedup.WordTokenizer
import sampleclean.clean.dedup.BlockingStrategy
import sampleclean.clean.dedup.BlockingKey

/*This class provides the main driver for the SampleClean
* application. We execute commands read from the command 
* line
*/
object SCDriver {

  //Configure the prompt for the sampleclean system
  val PROMPT:String = "sampleclean> ";

  //Driver Key Words
  var QUIT_COMMAND:String = "quit";

  //Exit Codes
  val SUCCESS:Int = 0;
  val QUIT:Int = 1;
  val ERROR:Int = 2;
  
  /* This command executes input from the command line
  *  prompt. The execution returns an exit code which
  * determines the next action. 
  */
  def run(command:String, parser:SampleCleanParser):Int = {

  	//force the string to lower case
  	val commandL = command.toLowerCase(); 
  	
  	if (commandL == QUIT_COMMAND)
  		return 2;

  	//execute the command
  	parser.parseAndExecute(commandL);

  	return 0;
  }

  
  /*This is the main of the program which starts the application
  */
  def mainsc(args: Array[String]) {

  	val conf = new SparkConf();
    conf.setAppName("SampleClean Materialized View Experiments");
    conf.setMaster("local[4]");
    conf.set("spark.executor.memory", "4g");

    val sc = new SparkContext(conf);
    val scc = new SampleCleanContext(sc);
    val saqp = new SampleCleanAQP();
    val hiveContext = new HiveContext(sc);
    val p = new ParametricOutlier(scc);
    scc.closeHiveSession("src_sample")
    scc.initializeHive("src","src_sample",0.01)
    p.clean("src_sample", "key", 1.8)
    println(saqp.normalizedSCQuery(scc, "src_sample", "key", "sum","true", 0.01))
    println(saqp.rawSCQuery(scc, "src_sample", "key", "sum","true", 0.01))
    p.clean("src_sample", "key", 1.0)
    println(saqp.normalizedSCQuery(scc, "src_sample", "key", "sum","true", 0.01))
     println(saqp.rawSCQuery(scc, "src_sample", "key", "sum","true", 0.01))
    //hiveContext.hql("CREATE TABLE IF NOT EXISTS src (key INT, value STRING)")
    //hiveContext.hql("LOAD DATA LOCAL INPATH 'kv1.txt' OVERWRITE INTO TABLE src")
    //hiveContext.hql("CREATE TABLE IF NOT EXISTS dblp (value STRING)")
    //hiveContext.hql("LOAD DATA LOCAL INPATH 'dblp.txt' OVERWRITE INTO TABLE dblp")
    /*scc.closeHiveSession("dblp_sample")
    scc.initializeHive("dblp","dblp_sample",0.001)
    hiveContext.hql("select count(*) from dblp").collect().foreach(println)
    hiveContext.hql("select count(*) from dblp_sample_clean").collect().foreach(println)*/

    //println(saqp.rawSCQuery(scc, "src_sample", "key", "count","key > 300", 0.1))
    //println(saqp.normalizedSCQuery(scc, "dblp_sample", "value", "count","true", 0.001)) // To Sanjay: predicate cannot be empty
    //p.clean("src_sample", "key", 1)


    val d = new Deduplication(scc);
    val sampleKey = new BlockingKey(Seq(2),WordTokenizer())
    val fullKey = new BlockingKey(Seq(0),WordTokenizer())

    val f1 = new Feature(Seq(2), Seq(0), Seq("Jaro", "JaccardSimilarity"))
    d.clean("dblp", "dblp_sample", BlockingStrategy("Jaccard", 0.8, sampleKey, fullKey), FeatureVectorStrategy(Seq(f1)))
    println(saqp.normalizedSCQuery(scc, "dblp_sample", "value", "count","true", 0.001))
    
    //println(saqp.rawSCQuery(scc, scc.updateTableDuplicateCounts("src_sample", hiveContext.hql("select src_sample_clean.hash as hash, 2 as dup from src_sample_clean limit 10")), "key", "count","key > 300", 0.01))
    /*hiveContext.hql("select * from src_sample_clean").collect().foreach(println)*/
    //scc.filterWriteTable("src_sample", hiveContext.hql("select * from src_sample_clean limit 10"))
    //println(saqp.rawSCQuery(scc, "src_sample_clean", "key", "count","key > 200", 0.01))
    //hiveContext.hql("select * from src_sample_clean").collect().foreach(println)
    //scc.updateHiveTableDuplicateCounts("src_sample", hiveContext.hql("select src_sample_clean.hash as hash, 1 as dup from src_sample_clean"))
    //println(saqp.rawSCQuery(scc, "src_sample_clean", "key", "count","key > 200", 0.01))
    //hiveContext.hql("select * from src_sample_clean").collect().foreach(println)

// Queries are expressed in HiveQL
  //println(saqp.approxQuery(hiveContext, "src_sample_clean", "key", "count","key > 200", 0.1))

   /* val parser:SCParse = new SCParse(sc);

  	//The REPL of the program
	  var input = readLine(PROMPT);
	  var exit_status = run(input,parser);
	  while (exit_status == 0){
		  input = readLine(PROMPT);
		  exit_status = run(input,parser);
	  }*/

  }

  def main(args: Array[String]) {

    val conf = new SparkConf();
    conf.setAppName("SampleClean Materialized View Experiments");
    conf.setMaster("local[4]");
    conf.set("spark.executor.memory", "4g");

    val sc = new SparkContext(conf);
    val scc = new SampleCleanContext(sc);
    val saqp = new SampleCleanAQP();
    val hiveContext = new HiveContext(sc);

    hiveContext.hql("CREATE TABLE IF NOT EXISTS dblp (value STRING)")
    hiveContext.hql("LOAD DATA LOCAL INPATH 'dblp.txt' OVERWRITE INTO TABLE dblp")
    scc.closeHiveSession("dblp_sample")
    scc.initializeHive("dblp","dblp_sample",0.001)
    hiveContext.hql("select count(*) from dblp").collect().foreach(println)
    hiveContext.hql("select count(*) from dblp_sample_clean").collect().foreach(println)

    //println(saqp.rawSCQuery(scc, "src_sample", "key", "count","key > 300", 0.1))
    //println(saqp.normalizedSCQuery(scc, "dblp_sample", "value", "count","true", 0.001)) // To Sanjay: predicate cannot be empty
    //p.clean("src_sample", "key", 1)


    val d = new Deduplication(scc);
    val sampleKey = new BlockingKey(Seq(2),WordTokenizer())
    val fullKey = new BlockingKey(Seq(0),WordTokenizer())

    val f1 = new Feature(Seq(2), Seq(0), Seq("Jaro", "JaccardSimilarity"))
    d.clean("dblp", "dblp_sample", BlockingStrategy("Jaccard", 0.8, sampleKey, fullKey), FeatureVectorStrategy(Seq(f1)))
    //println(saqp.normalizedSCQuery(scc, "dblp_sample", "value", "count","true", 0.001))

    //println(saqp.rawSCQuery(scc, scc.updateTableDuplicateCounts("src_sample", hiveContext.hql("select src_sample_clean.hash as hash, 2 as dup from src_sample_clean limit 10")), "key", "count","key > 300", 0.01))
    /*hiveContext.hql("select * from src_sample_clean").collect().foreach(println)*/
    //scc.filterWriteTable("src_sample", hiveContext.hql("select * from src_sample_clean limit 10"))
    //println(saqp.rawSCQuery(scc, "src_sample_clean", "key", "count","key > 200", 0.01))
    //hiveContext.hql("select * from src_sample_clean").collect().foreach(println)
    //scc.updateHiveTableDuplicateCounts("src_sample", hiveContext.hql("select src_sample_clean.hash as hash, 1 as dup from src_sample_clean"))
    //println(saqp.rawSCQuery(scc, "src_sample_clean", "key", "count","key > 200", 0.01))
    //hiveContext.hql("select * from src_sample_clean").collect().foreach(println)

    // Queries are expressed in HiveQL
    //println(saqp.approxQuery(hiveContext, "src_sample_clean", "key", "count","key > 200", 0.1))

    /* val parser:SCParse = new SCParse(sc);

     //The REPL of the program
     var input = readLine(PROMPT);
     var exit_status = run(input,parser);
     while (exit_status == 0){
       input = readLine(PROMPT);
       exit_status = run(input,parser);
     }*/

  }


}