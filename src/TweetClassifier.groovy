import org.knallgrau.utils.textcat.TextCategorizer;

import com.cybozu.labs.langdetect.Detector;
import com.cybozu.labs.langdetect.DetectorFactory;
import com.cybozu.labs.langdetect.LangDetectException;

import groovy.io.FileType;

/* Preprocess the twitter corpus in the directory data. The tweets will be 'analyzed' and copied
 * as follows:
 * 
 *  - links and no text 		-> classified/topic-of-tweet/links-only
 *  - English text and links 	-> classified/topic-of-tweet/links-and-text
 *  - English text  			-> classified/topic-of-tweet/text-only
 *  - other language			-> ignored
 */
class TweetClassifier {

	public static void main( args ){
		
		def rootDir = new File( "data/twitter/" )
		
		println "Start processing ${rootDir.getAbsolutePath()}"
		
		TextCategorizer categorizer = new TextCategorizer()
		
		DetectorFactory.loadProfile("profiles");
		
		def langStats = []
		
		def isEnglish = { s ->
			
			try{
				Detector detector = DetectorFactory.create()
				detector.append(s)
				def lang = detector.detect()
				langStats.add(lang)
				return lang == "en"
			} catch( LangDetectException e ){
				langStats.add("?")
				return false
			}
			
		}
		
		def hasHyperlink = { String s -> s.contains("http:") || s.contains("https:")}
		
		def containsOnlyLinks = { String s ->
			s.trim().split(" ").inject(true){ x,y -> x && hasHyperlink( y )  }
		}
		
		def copyTweet = { File tweetFile, String text, String category -> 
			
		   def topic =	tweetFile.getAbsolutePath().split("/")[-3]
		   
		   def toDir = new File("classified/${topic}/${category}/")
		   toDir.mkdirs()
		   
		   new File(toDir,tweetFile.getName() ).withWriter { it.write(text) }
		   
		}
		
		/* the following data structure contains the classification rules: */
		def classifierChain = [ [clazz:"links-only", rules:[containsOnlyLinks]],
								[clazz:"links-and-text", rules:[isEnglish,hasHyperlink]],
								[clazz:"text-only", rules:[isEnglish]] ]

		rootDir.eachFileRecurse( FileType.FILES ) { File tweetFile ->

			def tweet = tweetFile.getText()
			
			classifierChain.any{ classifier ->
				
				boolean isClass = classifier.rules.every{ it.call(tweet) }
				
				if(isClass) copyTweet(tweetFile,tweet,classifier.clazz)
			
				return isClass
				
			}
							
		}	
		
		println "All tweets: ${langStats.size()}"
		println "Tweets by language:"
		println  langStats.groupBy { it }.collect{ "${it.key}=${it.value.size()}" }.join(",")
	
	}
	
}
