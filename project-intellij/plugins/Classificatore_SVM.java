


/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */




import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import ij.measure.ResultsTable;
import java.awt.TextArea;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import weka.classifiers.RandomizableClassifier;
import weka.classifiers.functions.SMO;
import weka.classifiers.bayes.NaiveBayes;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.classifiers.*;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.trees.RandomForest;
import weka.classifiers.lazy.IBk;
import weka.core.Instance;
import weka.core.SerializationHelper;



public class Classificatore_SVM implements PlugIn{

    
    String[] cl = new String[4]; //array di stringhe per contenere i nomi dei classificatori disponibili
    int classe;
    String choise;
    
    
    /// Inizializzazione classificatori
    SMO svm = new SMO();
    NaiveBayes nb = new NaiveBayes();   
    RandomForest rf = new RandomForest();
    IBk knn = new IBk();
    Classifier cls;
   
    
    boolean val = false;
    ResultsTable r;
    
    
    
   
    @Override
    public void run(String arg) {
               
        cl[0] = "Naive Bayes";
        cl[1] = "Random Forest";
        cl[2] = "K-nn";
        cl[3] = "SVM";
        
        
        
        
            GenericDialog g = new GenericDialog("Benvenuto!");
            g.addMessage("Benvenuto nel classificatore!");
            g.showDialog();
            
            
        
        try {
            
            GenericDialog scelta = new GenericDialog("Come procedere?");
            scelta.addMessage("Vuoi usare un classificatore gia' addestrato?");
            scelta.showDialog();
            val = scelta.wasOKed();
            
            if(val){
             
                GenericDialog save = new GenericDialog("Inserisci il nome del modello");
                save.addTextAreas(null, null, 1, 15);
                save.showDialog();
                TextArea b = save.getTextArea1();
                String string = b.getText();
                
                
                cls = (Classifier) SerializationHelper.read(string);
                GenericDialog p = new GenericDialog("Load");
                p.addMessage("Modello caricato!");
                p.showDialog();
                
                
            }else{
            
             GenericDialog pi = new GenericDialog("Inserisci il nome del file di train");
             pi.addTextAreas(null, null, 1, 20);
             pi.showDialog();
             TextArea a = pi.getTextArea1();
             String file = a.getText();
          
            //carico il dataset
            DataSource source = new DataSource(file + ".arff");
            Instances traindata = source.getDataSet();
           
            
            if(traindata == null){
                GenericDialog d = new GenericDialog("Attenzione");
                d.addMessage("Il file non e' stato caricato correttamente!");
                d.showDialog();
            }
            
        
        // setto la prima colonna del file come quella identificativa della classe    
        traindata.setClassIndex(0);

        /*scelta classificatore per la fase di trainig*/    
        GenericDialog cc = new GenericDialog("Scegli un classificatore");
        
        cc.addRadioButtonGroup("Classificatori disponibili:", cl, 5, 5, arg);
        cc.showDialog();
        
        choise = cc.getNextRadioButton();
                        
        GenericDialog p = new GenericDialog("Training");
        
        switch(choise){
        
        case("Naive Bayes"):  
            
            p.addMessage("Naive Bayes");
            p.showDialog();
            
            try{
               
                nb.buildClassifier(traindata);  // training del classificatore
                p.addMessage("Training eseguito!");
                p.addMessage("Salvare il modello?");
                p.showDialog();
                
                boolean c = p.wasOKed();
                
                    if(c){

                        GenericDialog save = new GenericDialog("Inserisci il nome del modello");
                        save.addTextAreas(null, null, 1, 15);
                        save.showDialog();
                        TextArea b = save.getTextArea1();
                        String string = b.getText();

                        SerializationHelper.write(string, nb);
                        GenericDialog d = new GenericDialog(" ");
                        d.addMessage("Modello salvato!");
                        d.showDialog();
                    }
                
                
            }catch(Exception e){
                
                    GenericDialog d = new GenericDialog("OPSS!");
                    d.addMessage("Qualcosa e' andato storto!");
                    d.showDialog();
            }
           
            
            
            break;
        case("Random Forest"):
            
            p.addMessage("Random Forest");
            p.showDialog();
             try{
                 
                rf.buildClassifier(traindata);
                p.addMessage("Training eseguito!");
                 p.addMessage("Salvare il modello?");
                p.showDialog();
                
                boolean c = p.wasOKed();
                
                    if(c){

                        GenericDialog save = new GenericDialog("Inserisci il nome del modello");
                        save.addTextAreas(null, null, 1, 15);
                        save.showDialog();
                        TextArea b = save.getTextArea1();
                        String string = b.getText();

                        SerializationHelper.write(string, rf);
                        GenericDialog d = new GenericDialog(" ");
                        d.addMessage("Modello salvato!");
                        d.showDialog();
                    }
               
            }catch(Exception e){
                
                    GenericDialog d = new GenericDialog("OPSS!");
                    d.addMessage("Qualcosa e' andato storto!");
                    d.showDialog();
            }
            break;
         case("K-nn"):
             
            p.addMessage("K-nn");
            p.showDialog();
            
             try{
                knn.buildClassifier(traindata);
                p.addMessage("Training eseguito!");
                p.showDialog();
                p.addMessage("Salvare il modello?");
                p.showDialog();
                
                boolean c = p.wasOKed();
                
                    if(c){

                        GenericDialog save = new GenericDialog("Inserisci il nome del modello");
                        save.addTextAreas(null, null, 1, 15);
                        save.showDialog();
                        TextArea b = save.getTextArea1();
                        String string = b.getText();

                        SerializationHelper.write(string, knn);
                        GenericDialog d = new GenericDialog(" ");
                        d.addMessage("Modello salvato!");
                        d.showDialog();
                    }
               
                }catch(Exception e){
                
                    GenericDialog d = new GenericDialog("OPSS!");
                    d.addMessage("Qualcosa e' andato storto!");
                    d.showDialog();
                }
        
            break;   
            
            case("SVM"):
            p.addMessage("SVM");
            p.showDialog();
             try{
                svm.buildClassifier(traindata);
                p.addMessage("Training eseguito!");
                p.showDialog();
                p.addMessage("Salvare il modello?");
                p.showDialog();
                
                boolean c = p.wasOKed();
                
                if(c){
                    
                    GenericDialog save = new GenericDialog("Inserisci il nome del modello");
                    save.addTextAreas(null, null, 1, 15);
                    save.showDialog();
                    TextArea b = save.getTextArea1();
                    String string = b.getText();
                    
                    SerializationHelper.write(string, svm);
                    GenericDialog d = new GenericDialog(" ");
                    d.addMessage("Modello salvato!");
                    d.showDialog();
                }
               
            }catch(Exception e){
                
                    GenericDialog d = new GenericDialog("OPSS!");
                    d.addMessage("Qualcosa e' andato storto!");
                    d.showDialog();
            }
        
            break;    
            
        }    
            
            
           }

            
            /**
             * Caricamento dei dati per il test
             */
            // TODO: l'utente deve scegliere il file per la fase di test
            GenericDialog po = new GenericDialog("Inserisci il nome del file di test");
             po.addTextAreas(null, null, 1, 20);
             po.showDialog();
             TextArea ab = po.getTextArea1();
             String file2 = ab.getText();
            
            
            DataSource source2 = new DataSource(file2 + ".arff");
            Instances testdata = source2.getDataSet();
            
           
            
            testdata.setClassIndex(0);
            
            GenericDialog c = new GenericDialog("Predictions");
            /**
             * Predizioni del classificatore
             */
            if(val){
                
                //Questo if classifica le istanze provenienti da un modello precedentemente salvato        
                
                for (int j=0;j<testdata.numInstances();j++){
               
                Instance newInst = testdata.instance(j);
                
                double preNB = cls.classifyInstance(newInst);
                String predString = testdata.classAttribute().value((int) preNB);
                
                
                c.addMessage("L'istanza n. " + (j+1) + " e' stata classificata come: " + predString);
     
                }
             c.showDialog();
            }
             
        switch(choise){
        
        case("Naive Bayes"):  
            
            r = new ResultsTable(1);
            
                for (int j=0;j<testdata.numInstances();j++){
                
                    Instance newInst = testdata.instance(j);

                    double preNB = nb.classifyInstance(newInst);
                    String predString = testdata.classAttribute().value((int) preNB);

                    c.addMessage("L'istanza n. " + (j+1) + " e' stata classificata come: " + predString);

                    r.addValue("Istanza", j+1);
                    r.addValue("Etichetta", predString);
                    r.incrementCounter();
                }
                
            r.deleteRow(r.getCounter()-1);
            r.show("Risultati");
            // c.showDialog();
            
            break;
        case("Random Forest"):
            
            r = new ResultsTable(1);
            
                for (int j=0;j<testdata.numInstances();j++){

                    Instance newInst = testdata.instance(j);

                    double preNB = rf.classifyInstance(newInst);
                    String predString = testdata.classAttribute().value((int) preNB);

                    r.addValue("Istanza", j+1);
                    r.addValue("Etichetta", predString);
                    r.incrementCounter();
                   // c.addMessage("L'istanza n. " + (j+1) + " e' stata classificata come: " + predString);

            }
                
            r.deleteRow(r.getCounter()-1);
            r.show("Risultati");    
            //c.showDialog();
           
         case("K-nn"):
             
             r = new ResultsTable(1);
             
                for (int j=0;j<testdata.numInstances();j++){
               
                    Instance newInst = testdata.instance(j);

                    double preNB = knn.classifyInstance(newInst);
                    String predString = testdata.classAttribute().value((int) preNB);

                    r.addValue("Istanza", j+1);
                    r.addValue("Etichetta", predString);
                    r.incrementCounter();
                    //c.addMessage("L'istanza n. " + (j+1) + " e' stata classificata come: " + predString);
     
                }
               
            r.deleteRow(r.getCounter()-1);
            r.show("Risultati");  
            //c.showDialog();
            
            break;
         case("SVM"):
             
             r = new ResultsTable(1);
             
                for (int j=0;j<testdata.numInstances();j++){
               
                    Instance newInst = testdata.instance(j);

                    double preNB = svm.classifyInstance(newInst);
                    String predString = testdata.classAttribute().value((int) preNB);

                    r.addValue("Istanza", j+1);
                    r.addValue("Etichetta", predString);
                    r.incrementCounter();
                    //c.addMessage("L'istanza n. " + (j+1) + " e' stata classificata come: " + predString);
     
                }
               
            r.deleteRow(r.getCounter()-1);
            r.show("Risultati");  
            //c.showDialog();
            
            break;    
        
        }
        

            
    } catch (Exception ex) {
            Logger.getLogger(Classificatore_.class.getName()).log(Level.SEVERE, null, ex);
        }
      
        
    }
    

   
    
}
