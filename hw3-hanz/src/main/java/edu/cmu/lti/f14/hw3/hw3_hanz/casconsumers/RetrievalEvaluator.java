package edu.cmu.lti.f14.hw3.hw3_hanz.casconsumers;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.ProcessTrace;

import edu.cmu.lti.f14.hw3.hw3_hanz.typesystems.Document;
import edu.cmu.lti.f14.hw3.hw3_hanz.typesystems.Token;
import edu.cmu.lti.f14.hw3.hw3_hanz.utils.Utils;



public class RetrievalEvaluator extends CasConsumer_ImplBase {

	/** query id number **/
	public ArrayList<Integer> qIdList;

	/** query and text relevant values **/
	public ArrayList<Integer> relList;
	public ArrayList<String> textList;
	public ArrayList<HashMap<String,Double>>vec;
	public ArrayList<HashMap<String,Double>>vc;
	
	public void initialize() throws ResourceInitializationException {

		qIdList = new ArrayList<Integer>();
		relList = new ArrayList<Integer>();
		textList=new ArrayList<String>();
		vec=new ArrayList<HashMap<String,Double>>();
		vc=new ArrayList<HashMap<String,Double>>();

	}

	/**
	 * 1.construct the global word dictionary 2. keep the word
	 * frequency for each sentence
	 */
	@Override
	public void processCas(CAS aCas) throws ResourceProcessException {

		JCas jcas;
		try {
			jcas =aCas.getJCas();
		} catch (CASException e) {
			throw new ResourceProcessException(e);
		}

		FSIterator it = jcas.getAnnotationIndex(Document.type).iterator();
	
		if (it.hasNext()) {
			Document doc = (Document) it.next();

			//Make sure that your previous annotators have populated this in CAS
			FSList fsTokenList = doc.getTokenList();
			String t=doc.getText();
			textList.add(t);
			
			
			ArrayList<Token>tokenList=Utils.fromFSListToCollection(fsTokenList, Token.class);
			HashMap<String,Double>tmp=collectionToVector(tokenList);
			vec.add(tmp);
			vc.add(tmp);

			qIdList.add(doc.getQueryID());
			relList.add(doc.getRelevanceValue());
			

		}

	}

	/**
	 * 1. Compute Cosine Similarity and rank the retrieved sentences 2.
	 * Compute the MRR metric
	 */
	@Override
	public void collectionProcessComplete(ProcessTrace arg0)throws ResourceProcessException, IOException {

		super.collectionProcessComplete(arg0);
		BufferedWriter writer=new BufferedWriter(new FileWriter("report.txt"));
		ArrayList<Integer> rank=new ArrayList<Integer>();
		for (int i = 0; i < qIdList.size();) {
		  int qId = qIdList.get(i);
		  int j = i + 1;
		  HashMap<String, Double> queryVector = vec.get(i);
		  ArrayList<Tuple> result = new ArrayList<Tuple>();
		  
		  while (j < qIdList.size() && qIdList.get(j) == qId) {
		    HashMap<String, Double> docVector = vec.get(j);
		    
		    /**
		     * we can use different similarity function here
		     */
		    
		    //double score=bm25(queryVector,docVector,i)
		    
		    double score = computeCosineSimilarity(queryVector, docVector);
		    Tuple pair = new Tuple(score, j);
		    result.add(pair);
		    j++;
		  }
		  
		  /**
		   * get the ranks
		   */
		  Collections.sort(result,new TupleComparator());
		  for (int ii=0;ii<result.size();ii++){
		    int ind=result.get(ii).index;
		    if(relList.get(ind)==1){
		      String cosine=String.format("%.4f", result.get(ii).score);
		      writer.write("cosine="+cosine+"\track="+(ii+1)+"\tqid="+qId+"\trel=1\t"+textList.get(ind)+"\n");
		      rank.add(ii+1);
		      break;
		    }
		  }
		  i=j;
    
		}
		/*
		 * comput mrr
		 */
		double metric_mrr = compute_mrr(rank);
		String out=String.format("%.4f",metric_mrr);
		writer.write("MRR="+out);
		writer.close();
		
		//System.out.println(" (MRR) Mean Reciprocal Rank ::" + metric_mrr);
	}
	
	private HashMap<String, Double> collectionToVector(ArrayList<Token> input) {
    HashMap<String, Double> map = new HashMap<String, Double>();
    for (Token token : input) {
      map.put(token.getText(), (double) token.getFrequency());
    }
    return map;
  }

	/**
	 * 
	 * @return cosine_similarity
	 */
	private double computeCosineSimilarity(Map<String, Double> queryVector,
			Map<String, Double> docVector) {
		double cosine_similarity = 0.0;
    double queryLen = 0.0, docLen = 0.0, innerProd = 0.0;

    for (String key : queryVector.keySet())
      queryLen += Math.pow(queryVector.get(key), 2);

    for (String key : docVector.keySet()) {
      docLen += Math.pow(docVector.get(key), 2);
      if (queryVector.containsKey(key)) {
        innerProd += docVector.get(key) * queryVector.get(key);
      }
    }

    cosine_similarity = innerProd / Math.sqrt((queryLen * docLen));

    return cosine_similarity;
	}

	/**
	 * 
	 * @return mrr
	 */
	private double compute_mrr(ArrayList<Integer> rank) {
	  double metric_mrr = 0.0;
    for (int i = 0; i < rank.size(); i++) {
      metric_mrr += 1.0 / (double) rank.get(i);
    }
    return metric_mrr / (double) rank.size();
		
	
	}
	/**
	 * basic bm25
	 * @param queryVector
	 * @param docVector
	 * @param index
	 * @return
	 */
	private double bm25(Map<String, Double> queryVector, Map<String, Double> docVector, int index) {

    transformToIdf();
    double score = 0.0;
    double k1 = 1.2, b = 0.5;
    int docLen = 0;

    for (String key : docVector.keySet()) {
      docLen += docVector.get(key);
    }

    for (String key : queryVector.keySet()) {
      double tf = docVector.containsKey(key) ? docVector.get(key) : 0;
      score += vc.get(index).get(key) * tf * (k1 + 1.0) / (tf + k1 * (1.0 - b + b * docLen));
    }

    return score;
  }
	
	/*
	 * this class is used to store the scores for each doc
	 */
	public class Tuple {
    public double score;

    public int index;

    public Tuple(double score, int index) {
      this.score = score;
      this.index = index;
    }
  }
	
	/*
	 * simple compare
	 */
	public class TupleComparator implements Comparator<Tuple> {
    @Override
    public int compare(Tuple p1, Tuple p2) {
      if (p1.score > p2.score)
        return -1;
      else if (p1.score < p2.score)
        return 1;
      return 0;
    }
  }
	/*
   * this can be use in task2 to improve similarity
   */
	 public void transformTfIdf() {
	    for (int i = 0; i < vec.size();) {
	      HashMap<String, Double> df = new HashMap<String, Double>();
	      int qId = qIdList.get(i);
	      int j = i;
	      
	      while (j < qIdList.size() && qIdList.get(j) == qId) {
	        HashMap<String, Double> vecc = vec.get(j);
	        for (String key : vecc.keySet()) {
	          if (df.containsKey(key))
	            df.put(key, df.get(key) + 1);
	          else
	            df.put(key, 1.0);
	        }
	        j++;
	      }

	      int docSize = j - i + 1;
	      for (int k = i; k < j; k++) {
	        HashMap<String, Double> tmpVec = vec.get(k);
	        for (String key : tmpVec.keySet()) {
	          double docFreq = df.get(key);
	          tmpVec.put(key, tmpVec.get(key) * Math.log10((double) docSize / docFreq));
	        }
	      }
	      i = j;
	    }
	  }
	 /*
	  * use for bm25
	  */
	 public void transformToIdf() {
	    for (int i = 0; i < vec.size();) {
	      HashMap<String, Double> df = new HashMap<String, Double>();

	      int qId = qIdList.get(i);
	      int j = i;
	      while (j < qIdList.size() && qIdList.get(j) == qId) {
	        HashMap<String, Double> vecc = vec.get(j);
	        for (String key : vecc.keySet()) {
	          if (df.containsKey(key))
	            df.put(key, df.get(key) + 1);
	          else
	            df.put(key, 1.0);
	        }
	        j++;
	      }

	      int docSize = j - i + 1;
	      for (int k = i; k < j; k++) {
	        HashMap<String, Double> tmpVec = vc.get(k);
	        for (String key : tmpVec.keySet()) {
	          double docFreq = df.get(key);
	          tmpVec.put(key, Math.log10((double) docSize / docFreq));
	        }
	      }
	      i = j;
	    }
	  }

	

}
