package edu.jhu.hyperclass

import scala.collection.JavaConversions._
import redis.clients.jedis._


/**
 * Wrapper around Redis that check a bloomfilter before querying
 */
class RedisPPDB(host: String, port: Int) { 
	import edu.jhu.jerboa.counting.BloomFilter

	private lazy val redis = {
		val red = new Jedis(host, port)
		if (red.ping != "PONG")
			throw new RuntimeException("cannot connect to redis! host/port:" + host+"/"+port )
		red
	}

	private lazy val bf = {
		val ppdbkeys = new BloomFilter(BloomFilter.percentFreeMemory(0.4), redis.dbSize)
		redis.keys("*").foreach(ppdbkeys.set(_))
		ppdbkeys
	}
		
	def query(key: String): Seq[PPDBRule] = {
		if (bf.in(key)) {
			val maxRHSs = 1000
			val values = redis.lrange(key, 0, maxRHSs)
			values.map(ruleStr => PPDBRuleIO.str2rule(ruleStr))
		}
		else Seq()
	}

}

object PPDBRuleIO {
	import scala.collection.mutable.HashMap

	// TODO fix this, only tab is supported now
	val sep1 = "\t"
	val sep2 = "\t"
	val eq = "\t"

	def str2rule(redisStr: String): PPDBRule = {
		val ar = redisStr.split("\t")
		val rhs = ar(0)
		val parent = ar(1)
		val props = new HashMap[String, String]
		assert(ar.length % 2 == 0)
		for(i <- 2 until ar.length by 2)
			props.update(ar(i), ar(i+1))
		new PPDBRule(rhs, parent, props)
	}

	def validToken(s: String) = s.indexOf(sep1) < 0 && s.indexOf(sep2) < 0 && s.indexOf(eq) < 0

	private def sortedKVs(rule: PPDBRule): String = rule.properties.map(kv => kv._1 + eq + kv._2).toBuffer.sorted.mkString(sep2)

	def rule2str(rule: PPDBRule): String = {
		if(!validToken(rule.rhs))
			println("bad rule.rhs: " + rule.rhs)
		assert(validToken(rule.rhs))
		assert(validToken(rule.parent))
		rule.properties.foreach(kv => {
			if(!validToken(kv._1) || !validToken(kv._2))
				println("bad: " + kv)
			assert(validToken(kv._1))
			assert(validToken(kv._2))
		})
		List(rule.rhs, rule.parent, sortedKVs(rule)).mkString(sep1)
	}
}

// lhs is the key
class PPDBRule(val rhs: String, val parent: String, allProperties: scala.collection.Map[String, String]) {

	val neededKeys = Set("p(LHS|e)", "p(LHS|f)", "p(e|LHS)", "p(e|f)", "p(e|f,LHS)", "p(f|LHS)", "p(f|e)", "p(f|e,LHS)")
	val properties = allProperties.filterKeys(k => neededKeys.contains(k))

	lazy val score = neededKeys.map{ k => -allProperties(k).toDouble }.sum
	override def toString = PPDBRuleIO.rule2str(this)
}


// code needed to insert into redis
object RedisPPDBSetup {

	def main(args: Array[String]) {
		val (port, host, ppdb) = if(args.length == 0) {
			(6379, "r4n15", "/export/common/SCALE13/Text/ppdb/scored-ppdb-1.0.lexical.gz")
			} else (args(0).toInt, args(1), args(2))

		println("[redis insert] trying to connect to redis at %s on port %d".format(host, port))
		val redis = new Jedis(host, port)
		val reader = getReader(ppdb)

		//println("[redis insert] flushing existing stuff...")
		//val t = redis.multi
		//t.flushDB
		//t.exec
		println("for some reason, cannot flush programatically, do so manually")

		println("[redis insert] inserting stuff...")
		//val pipe = redis.pipelined
		var i = 0
		val start = System.currentTimeMillis
		var last = start
		while(reader.ready) {
			val line = reader.readLine
			val ar = line.split(" \\|\\|\\| ")
			val parent = ar(0).toLowerCase
			val leftChild = ar(1).toLowerCase
			val rightChild = ar(2).toLowerCase
			val properties = str2map(ar(ar.length-1))

			val key = leftChild
			val value = PPDBRuleIO.rule2str(new PPDBRule(rightChild, parent, properties))
			//val value = List(rightChild, properties("p(LHS|e)"), properties("p(LHS|f)"),
			//				properties("p(e|LHS)"), properties("p(e|f)"), properties("p(e|f,LHS)"),
			//				properties("p(f|LHS)"), properties("p(f|e)"), properties("p(f|e,LHS)"))
			//println("key = %s, value = [%s]".format(key, value))
			//redis.rpush(key, value: _*)
			redis.rpush(key, value)
			//pipe.rpush(key, value)

			val step = 15000
			if(i % step == 0) {
				val now = System.currentTimeMillis
				val recent = step.toDouble / (now-last)
				val avg = i.toDouble / (now-start)
				println("i=%d, %.1f K lines/sec recent, %.1f K lines/sec avg".format(i, recent, avg))
				//println("adding(%d) %s => %s (%.1f %.1f K lines/sec recent/avg)".format(i, key, properties, recent, avg))
				last = now
			}
			i += 1
		}
		reader.close
		//pipe.exec

		println("done, added %d rules in %.1f minutes".format(i, (System.currentTimeMillis - start)/(60d*1000d)))
	}

	private def str2map(kvs: String): Map[String, String] = {
		val properties = kvs.split(" ").flatMap(kv => {
			val x = kv.split("=")
			if(x.length != 2) {
				//println("kvs = " + kvs)
				//throw new RuntimeException("x = [%s]".format(x.mkString(", ")))
				Seq()
			}
			else Seq((x(0), x(1)))
		}).toMap

		// get rid of trailing 0s
		properties.mapValues(value => """\.0*$""".r.replaceAllIn(value, ""))
	}

	private def getReader(filename: String): java.io.BufferedReader = {
		val file = new java.io.File(filename)
		val is = new java.io.FileInputStream(file)
		val gis = new java.util.zip.GZIPInputStream(is)
		val isr = new java.io.InputStreamReader(is, "UTF-8")
		return new java.io.BufferedReader(isr)
	}
}



/* vim: set noet : */
