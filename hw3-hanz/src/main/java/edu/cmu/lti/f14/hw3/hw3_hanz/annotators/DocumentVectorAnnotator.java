package edu.cmu.lti.f14.hw3.hw3_hanz.annotators;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.IntegerArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.tcas.Annotation;

import edu.cmu.lti.f14.hw3.hw3_hanz.typesystems.Document;
import edu.cmu.lti.f14.hw3.hw3_hanz.typesystems.Token;
import edu.cmu.lti.f14.hw3.hw3_hanz.utils.StanfordLemmatizer;
import edu.cmu.lti.f14.hw3.hw3_hanz.utils.Utils;

public class DocumentVectorAnnotator extends JCasAnnotator_ImplBase {

	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {

		FSIterator<Annotation> iter = jcas.getAnnotationIndex().iterator();
		if (iter.isValid()) {
			iter.moveToNext();
			Document doc = (Document) iter.get();
			createTermFreqVector(jcas, doc);
		}

	}

	/**
   * A basic white-space tokenizer, it deliberately does not split on punctuation!
   *
	 * @param doc input text
	 * @return    a list of tokens.
	 */

	List<String> tokenize0(String doc) {
	  List<String> res = new ArrayList<String>();
	  
	  for (String s: doc.split("\\s+"))
	    res.add(s);
	  return res;
	}
	
	/*
	 * use stopwords, stems, lowercase to improve token vector
	 */
	List<String> tokenize_hanz(String doc) throws FileNotFoundException {

    List<String> res = new ArrayList<String>();
    BufferedReader br = new BufferedReader(new FileReader("stopwords.txt"));
    HashSet<String> word_list = new HashSet<String>();
    String line = "";
    
    try {
      while ((line = br.readLine()) != null)
        word_list.add(line);
      br.close();
    } catch (IOException e) {
      e.printStackTrace();
    }

    for (String s : doc.split("\\s+")) {
      /**
       * normalize
       */
      String tmp = s.replaceAll("[^a-zA-Z ]", "").toLowerCase();
      /**
       * stemming
       */
      String stemword = StanfordLemmatizer.stemWord(tmp);
      /**
       * get rid of stop words
       */
      
      if (!word_list.contains(stemword))
        res.add(stemword);
    }

    return res;
  }

	/**
	 * This will get the word vector in each document
	 * @param jcas
	 * @param doc
	 */

	private void createTermFreqVector(JCas jcas, Document doc) {

		String docText = doc.getText();
		
		//TO DO: construct a vector of tokens and update the tokenList in CAS
    //TO DO: use tokenize0 from above 
		List<String> a=new ArrayList<String>();
		a=tokenize0(docText);
		HashMap<String, Token> t=new HashMap<String, Token>();
		/**
		 * get all the frequency
		 */
		for(int i=0;i<a.size();i++){
		  String word=a.get(i);
		  Token tt=t.get(word);
		  if(tt==null){
		    Token t_new=new Token(jcas);
		    t_new.setText(word);
		    t_new.setFrequency(1);
		    t.put(word, t_new);
		  }
		  else{
		    tt.setFrequency(tt.getFrequency()+1);
		    t.put(word, tt);
		    
		  }
		}
		ArrayList<Token>arr=new ArrayList<Token>();
		for(String s : t.keySet()){
		  arr.add(t.get(s));
		}
		doc.setTokenList(Utils.fromCollectionToFSList(jcas, arr));

	}

}
