// This program is distributed under the terms of the GNU General Public License Version 2
// Copyright 2010,2012,2015 Andrew Strain. 

/* trumco.java Parses command line, coordinates reading and processing of input sample
	 renders plots, draws axis, and delivers output to bmpout */

import java.io.*;

public final class trumco     
{   
    int strtSpct,finiSpct,duraSpct;
    int mssrw,mssrStep,chnksteps; 
    int chnlScan,samStep;
    int sampyrate,rmngsteps,maplap;
    double stagger=0d;
    double tplots;
  
    int thcol=0x00108028, thblk=0x00d07060, tvcol=0x00b09090, tdcol=0x008080b8;
    int thlmd=clrmrg(thcol,thblk,0.5f);
    boolean freqdrawn=false;

    String filenam,pofix;
    takeWavb tookWav;
    short[] insamps;
    int preadsz=12288;
    int dochunks;
  
    int[] BmapSrc,Viso;
    int Bmapcol,btmpnum;
    
    int totframs;
    long timerstrt;

    int axHght,axWdth,pltLen,pltHgt;
    double gamma,gscale;
    double strtd,stepsz;
    double minWv;
    int fsunit=0;
    double sclfac=0;
    double maxWv;
    boolean logfsc=true;
      
  public static void main(String[] args) /* main */
  { trumco strt = new trumco(args);  //creates instance of self for nonstatic context hooha 
    System.exit(0); }                 

  public trumco(String[] args)         /* constructor */
  { timerstrt = System.currentTimeMillis();
 
    byte argLen=(byte)args.length;
    if(argLen<1)
    { poutln("  Spictrogram recieved no parameters.");
      args = new String[1]; args[0]="?"; }
    
    if ( args[0].endsWith("?") | args[0].equalsIgnoreCase("help") )
    { poutln(helpnotes);
	    System.exit(1); }

    if(!((args[0].endsWith("v")|args[0].endsWith("V")))&&!(args[1].equalsIgnoreCase("-rawfl")))
    { poutln("  No filename to operate on...");
	    poutln("  Usage: Java -jar trumco.jar [filename]\n");
	    System.exit(1); }

                       //   0        1        2        3        4
    String swtches[] ={ "-pltht","-pltln","-lpsmp","-lptim","-mrsmp", //mrsmp //mrtim //lesmp //letim
                        "-mrtim","-mxamp","-minwv","-stspc","-fnspc",
                        "-chnnl","-dursp","-msspc","-aplts","-tprpl",
                        "-maxhz","-minhz","-maxwv","-gamma","-ovlap",
                        "-sdith","-pxamp","#22   ","#23   ","#24   ",
                        "#25   ","#26   ","#27   ","#28   ","#29   "      
                        };

                       //   0        1        2        3        4
    String swtchez[] ={ "-dwnmx","-dffmx","-noaxt","-noaxf","-logfs",
                        "-fiwvl","-rleap","-ovrwt","-pntdt","-spred",
                        "-outfi","      ","-namex" 
                        }; //namex must be last switch due to a little cludge
                     
                
    double paramos[] = new double[(swtches.length+swtchez.length)];

    for(int f=0; f<paramos.length ; f++){ paramos[f]=-1; }
    paramos[10]=0;   //channel to scan
    paramos[12]=0;   //sample stepping hazardous
  
    /* innerparameter defaults*/
    gamma=0.8d;    gscale=0.33d;
    pltLen=800;    pltHgt=500;   maplap=0; 
    minWv=3d;       mssrw= 450;   double autpltval=1;  
    mssrStep=0;     
    strtSpct=0;    finiSpct=-1;
    chnlScan=0;    duraSpct=-1;
    samStep=0;
    stepsz=mssrw/2;  
    
    pofix=""; //string to add to output filename
    axHght=9; axWdth=24;  //size of axis
    filenam = args[0].substring( 0, (args[0].length()-4) );                     
                       
    /*open file and read in necessary parameters!*/
    poutln("");
    
    if ((argLen>1)&&args[1].equalsIgnoreCase("-rawfl"))
    { int smrt=44100, chls=1, Bps=2; boolean endio=false;
      try
      { smrt=Integer.parseInt(args[2]);
        chls=Integer.parseInt(args[3]);
        Bps=Integer.parseInt(args[4]);
        if(args[5].equalsIgnoreCase("e")){ endio=true; } }
      catch (NumberFormatException e)
      { poutln("  Problem with rawfile parameters\n  (continuing with defaults)"); }
      catch ( ArrayIndexOutOfBoundsException e)
      { poutln("  Incomplete rawfile parameters\n  (continuing with defaults)"); }      
      tookWav = new takeWavb(args[0], preadsz, smrt, chls, Bps, endio); 
		}
    else
    { tookWav = new takeWavb(args[0], preadsz);
      if(tookWav.totlFrames==0){ System.exit(0); } }
    sampyrate=tookWav.Samprate;
    totframs=tookWav.totlFrames;
    finiSpct=totframs; strtSpct=0; duraSpct=-1;
    /*file opened*/
  
    /* parse command line */
    //for every possible switch, test all arguements....

    for(byte argdex=1; argdex<argLen; argdex++)
    { for(byte swtcdex=(byte)(swtches.length-1) ; swtcdex>-1; swtcdex--) 
      { if (swtches[swtcdex].equalsIgnoreCase(args[argdex])) 
        { if(argdex+1<argLen)
          { try{ paramos[swtcdex]=Double.parseDouble(args[argdex+1]); }
            catch (NumberFormatException e)
            { 
              System.out.println( e );
              System.out.println("  Problem with command switch "+paramos[swtcdex]+" "+swtcdex+" "+args[argdex+1]);
              System.out.println("  (continuing with default setting)");
              paramos[swtcdex]=0;              }
					}
          else{ paramos[swtcdex]=0; }
				}//System.out.println(paramos[swtcdex]+" "+swtches[swtcdex]+" "+swtcdex+" "+args[argdex]);  
			}//for each arguement
		} //for each swtch and paramo'ption

    for(byte swtcdex=(byte)(swtchez.length-1) ; swtcdex>-1; swtcdex--) 
    { for(byte argdex=1; argdex<argLen; argdex++) 
      { if (swtchez[swtcdex].equalsIgnoreCase(args[argdex])) 
        { paramos[swtcdex+swtches.length]=0; 
          if((swtcdex==swtchez.length-1)&&(argdex+1<argLen))
          { pofix=(args[argdex+1]); } 
				}//System.out.println(paramos[swtcdex]+" "+swtches[swtcdex]+" "+swtcdex+" "+args[argdex]);  
			}//for each arguement
		} //for each swtch and paramo

    /*configure internal parameters according to parsed inputs*/
    poutln("  Setting Plot Options....\n");
    if(paramos[0]>-1){ pltHgt=(int)paramos[0]; //-pltht
                       poutln("  Plot Height is changed to "+pltHgt); }
                 else{ poutln("  Plot Height default is "+pltHgt); }
    if(paramos[1]>-1){ pltLen=(int)paramos[1];     //-pltln
                       poutln("  Plot Width is changed to "+pltLen); }
                 else{ poutln("  Plot Width default is "+pltLen); }
    if(paramos[19]>-1){ maplap=(int)paramos[19]; 
                        poutln("  Plot overlap set to "+(int)maplap);
                        if(maplap>=pltLen)
                        { maplap=(pltLen*3)/4;
                          poutln("! Overlap was set too large. Reducing to 3/4 of Plot Length"); } }
    
    if(paramos[6] >-1)
    { gscale=paramos[6];
      if(gscale>1){ gscale=1; poutln("  Max Amp set too high,"); }
      if(gscale<1d/0x7fff){ gscale=1d/0x7ffe; poutln("  Max Amp set too low,");}
      poutln("  Rendered (white) amp > "+pdouble(100,gscale,0)+" ( "+(int)(gscale*0x7fff)+" @16bit )");
		}   //-mxamp 

    if(paramos[18]>-1){ gamma=paramos[18]; 
                        poutln("  Gamma factor set to "+pdouble(1000,gamma,0)); } 

    if(paramos[12]>0){ samStep=(int)paramos[12]; }  //-msspc
    boolean autoplt=false;
    if(paramos[20]>-1)
		{ stagger=paramos[20]; 
      poutln("  Step dither set to:"+pdouble(2,stagger,0));
      if(stagger>1){ stagger=1d; poutln("  Maximum step dither is 1"); } }
                      
    if(paramos[32]>-1){ axHght=0; poutln("  Not drawing Time Axis."); }  //noaxt
    if(paramos[33]>-1){ axWdth=0; poutln("  Not drawing Frequency Axis."); }  //noaxf  
    if(paramos[35]>-1){ fsunit=1; poutln("  Wavlengths shown on Freq Axis."); }  //axfwv
    if(paramos[34]>-1) //set and initialise frequency index
    { logfsc=true; 
      poutln("  Logarthmic frequency scale choosen"); }
    else
    { logfsc=false;
      poutln("  Linear frequency scale (default)");      }
    poutln("");
    
    /*discern channel to process*/
    if(paramos[30]>0|paramos[10]==2)
    { paramos[10]=2; poutln("  Downmix channel selected"); } //dwnmx 
    if(paramos[31]>0|paramos[10]==3)
    { paramos[10]=3; poutln("  Diffmix channel selected"); } //dffmx
    if(paramos[10]>0)
    { chnlScan=(int)paramos[10];
      if((tookWav.channels<2)&&(paramos[10]>0))
      { poutln("! Only single channel available.");
        chnlScan=0; }  //chnnl
      else
      { if(chnlScan==0){ poutln("  First channel selected."); } }
      if(chnlScan==1){ poutln("  Second channel selected."); }
		}  
    
    tookWav.setStep((byte)samStep); //sample stepping -a curio
    
    if(paramos[4]>-1){ mssrw=(int)paramos[4]; }
    if(paramos[5]>-1){ mssrw=(int)((sampyrate*paramos[5])/1000); }
    if(mssrw==0){ mssrw=1; }
    poutln("  Measurement Window set to "+mssrw+" samples"
          +" ("+pdouble(10,mssrw*1000/sampyrate,0)+" millisecs)");
    
    if(paramos[15]>-1) { minWv=sampyrate/paramos[15]; }
    if(paramos[16]>-1) { maxWv=sampyrate/paramos[16]; } 
    else               { maxWv=(double)mssrw*2.0d; }
    if(paramos[17]>-1) { maxWv=paramos[17]; } 
    if(paramos[7]>-1)  { minWv=paramos[7]; }          //-minWv
    
    if(maxWv<minWv){ double t=maxWv; maxWv=minWv; minWv=t; }
    if(maxWv==minWv){ maxWv+=0.01f; 
                      poutln("! Attempted to correct selected frequency plot-range"); }
    poutln("  Frequency Plot Range:\n"
          +"     "+pdouble(1000,(sampyrate/minWv),0)+" Hz ("+pdouble(1000,minWv,0)+"t)\n"
          +"  to "+pdouble(1000,(sampyrate/maxWv),0)+" Hz ("+pdouble(1000,maxWv,0)+"t)" );  

    if(logfsc)
    { sclfac=Math.pow(Math.E, Math.log((double)maxWv/minWv)/((double)pltHgt-1)); }
    else
    { sclfac=(double)((minWv*pltHgt-maxWv)/(maxWv-minWv)+pltHgt); }

    /*discern plot start and finnish times*/
    if(paramos[8]>-1){ strtSpct=(int)((paramos[8]*sampyrate*10d+5d)/10000d); }   //stscn
    if(paramos[9]>-1){ finiSpct=(int)((paramos[9]*sampyrate*10d+5d)/10000d); }   //fnscn
    if(paramos[11]>-1){ duraSpct=(int)((paramos[11]*sampyrate*10d+5d)/10000d); } //lnscn

    if (duraSpct>-1)
    { if(strtSpct>-1){ finiSpct=strtSpct+duraSpct; }
      else{ strtSpct=finiSpct-duraSpct; } }
    else{ duraSpct=finiSpct-strtSpct; }

    if(Math.abs(finiSpct-duraSpct-strtSpct)>1000
                |finiSpct>totframs+1000|strtSpct<0|strtSpct>totframs|duraSpct<0)
    { poutln("! Warning: Requested plot time range was invalid\n"); }

    if(strtSpct<0|strtSpct>totframs){ strtSpct=0; }
    if(finiSpct>totframs){ finiSpct=totframs; }
    if(finiSpct<strtSpct){ finiSpct=totframs; }
    duraSpct=finiSpct-strtSpct;
    if (duraSpct<1){ duraSpct*=-1;}
    poutln("  Plotting "+pdouble(100000,((double)duraSpct)/(sampyrate),0)+" seconds");
    poutln("  Starting at "+pdouble(100000,((double)strtSpct)/(sampyrate),0)+", ending at "
           +pdouble(100000,((double)finiSpct)/(sampyrate),0)+" secs");
    if(paramos[13]>-1){ autpltval=paramos[13]; } //-aplts autpltval holds timeperplot
    if(paramos[14]>-1){ autpltval=paramos[14]; autoplt=true;}//-tprpl,autpltval hold number of plots instead
    if(paramos[2]>-1){ stepsz=paramos[2]; autpltval=0; } //mleap 
    if(paramos[3]>-1){ stepsz=((paramos[3]*sampyrate)/1000); autpltval=0; } //mlept 

    if(paramos[6]==-1)
    { pout("  Max Amplitude estimate is ");
      gscale=ampfind();
      poutln(pdouble(1000,gscale,0));
      if(gscale<1d/0x7fff){ gscale=1d/0x7ffe; }
      
			if(paramos[21]==-1){ paramos[21]=0.3d; }
			if(paramos[21]>0)
      { gscale*=paramos[21]; 
        if(gscale>1){ gscale=1; }
        if(gscale<1d/0x7fff){ gscale=1d/0x7ffe; }
        poutln("  Multiplying by factor of "+pdouble(1000,paramos[21],0)+","); }
      poutln("  Rendered (white) amp > "+pdouble(1000,gscale,0)+" ( "+(int)(gscale*0x7fff)+" @16bit )");
		}
    
    if((paramos[2]>-1)|(paramos[3]>-1))
    { rmngsteps =((int)(0.1f+duraSpct/stepsz))+1;
      poutln("  Plot time step set to "+pdouble(100,stepsz,0)+" samples"
            +" ("+pdouble(10,stepsz*1000/sampyrate,0)+" millisecs)");
      if((pltLen>rmngsteps)&&(paramos[1]>-1))
      { poutln("! Plot width reduced to "+rmngsteps+" (for given time steps)" ); }
      if(pltLen>rmngsteps){ pltLen=rmngsteps; } //do it quiet if len never set
      if((paramos[13]>-1)|(paramos[14]>-1))
      { poutln("! Autoplot ignored because time step is set"); }
    }
           
    if (autpltval>0) //requires duraSpct,pltLen,sampyrate
    { pout("\n  Setting time render interval to");
      if(autoplt) //! autpltval can = timeperplot?
      { stepsz= (double)(autpltval*sampyrate)/(1000*pltLen); } //set step for milliseconds
      else                                                                  //or for plot quantity
      { if(autpltval>1){ stepsz=(double)duraSpct/(pltLen-1+((pltLen-maplap)*(autpltval-1)));}
        else{ stepsz=(double)duraSpct/(pltLen*autpltval-1); } } 
    
      if(paramos[36]>-1) // -rndst ,roundstep
      { stepsz=(double)((int)(stepsz+0.5d));
        if (stepsz==0){ stepsz=1; }
        pout("  "+stepsz+" (rounded),");
			}else{ pout(" "+pdouble(100,stepsz,0) ); }
      
			tplots=duraSpct/(stepsz*pltLen);
      if(tplots>1d){ tplots=(duraSpct-(stepsz*pltLen))/(stepsz*(pltLen-maplap))+1; }
    
      poutln(" samples\n  ("+pdouble(10,stepsz*1000/sampyrate,0)+" millisecs) to make "
           +pdouble(100,tplots,0)+" plots, ("+pdouble(1,(tplots*pltLen+1),0)+" measures)"); 
		}
   
    rmngsteps =((int)(0.1f+duraSpct/stepsz))+1; 
    if(maplap>rmngsteps){ maplap=0; }

    /*plot start and finnish times discerned*/
    /*poutln("\n  Taking "+rmngsteps+" steps across "+pdouble(1,duraSpct*1000d/sampyrate,0)
          +" millisecs"
          +"\n  Starting "+pdouble(10,strtSpct*1000d/sampyrate,0)+"ms"
          +", Ending "+pdouble(10,finiSpct*1000d/sampyrate,0)+"ms\n");
    */
    String ess="s";

    poutln("  Working....\n");
  
    transform Foamer = new transform( pltHgt, mssrw, gscale, minWv, gamma, sclfac, logfsc, stagger); 

    strtd=(double)(strtSpct);              //strtspct is starting sample
    double unwlkd=0;                       //unwlkd is count of unwalked data available
    int zeropad= ((mssrw+1)/2)-strtSpct;   //zeropad is meassure centering shift
    
    //calced here as the adjusted strtSpct
    //poutln("zeropad="+zeropad);
    /*zeropad the first samples of the first insamps*/ 
    if (zeropad>0)
    { //prepad
      insamps=new short[preadsz+zeropad];          //insamps increased
      short[] tmpsmps =tookWav.getchn(chnlScan);   //temporary read buffer
      for(int fl=0; fl<tmpsmps.length; fl++)                //
      { insamps[fl+zeropad]=tmpsmps[fl]; }
      unwlkd+=(double)(tmpsmps.length+zeropad);
		}
    else
    { //preskip
      tookWav.setSkip(0-zeropad);
      insamps=new short[preadsz];
      short[] tmpsmps =tookWav.getchn(chnlScan);
      for(int fl=0; fl<tmpsmps.length; fl++)
      { insamps[fl]=tmpsmps[fl]; }
      unwlkd+=(double)tmpsmps.length; 
		}
    
    chnksteps =(int)((stepsz+unwlkd-mssrw)/stepsz); //poutln("chnksteps:"+chnksteps);
    
    //equation behaviour
    //while unwlkd is less than mssrw, chnksteps will be 0
    //as soon as unwlkdg=mssrw chnksteps will be 1
    //as soon as unwlkdg =mssrw+stepsize, chnksteps will be 2
    
    //after performing chnksteps, chnksteps*stepsize is subtracted from unwlkd
    //if unwlkd= mssrw+stepsize (2steps) unwlkd may be negative if stepsize>mssrw
    //and readin size is almost consumed
    //negative unwlkd can be skipped
    
    if(chnksteps>pltLen/10){ chnksteps=pltLen/10+1; } //make first stroll little for timecalc
  
    double usein=chnksteps*stepsz; 
    //poutln("p chnksteps:"+chnksteps+" unwlkd:"+unwlkd+" usein:"+usein);
    unwlkd=unwlkd-usein;
    Bmapcol=0; btmpnum=0;
    if (rmngsteps<pltLen)   //case if just one plot required
    { pltLen=rmngsteps; }
    BmapSrc =new int[(pltHgt+axHght)*(pltLen+axWdth)];  

    int oprint=0; //progress counter
    int otimer=(int)(1+(4000*stepsz)/(pltHgt*mssrw));
    //System.out.print("  Progress...\n");
    
    int rsltAry[];
    boolean leadin=true;
  
    long tumstrt= System.currentTimeMillis();
    boolean tumtime=true;
    int chnksdn=0;
    while(rmngsteps>0) //rmngsteps updated in graphresults()
    { if(leadin)
      { leadin=false; } //insamps has been premade for lead-in
      else
      { if(unwlkd<-64d)
        { int skip=(int)-unwlkd;
          tookWav.setHop(skip);
          Foamer.skid(skip);
          //poutln(" skipped:"+(skip)); 
          unwlkd+=skip;       
				}
        insamps= tookWav.getchn(chnlScan);
        unwlkd= unwlkd+insamps.length;
        chnksteps= (int)((stepsz+unwlkd-mssrw)/stepsz);
        usein=(double)chnksteps*stepsz;
        //poutln("q chnksteps:"+chnksteps+" unwlkd:"+unwlkd+" usein:"+usein
        //      +" insampslen:"+insamps.length+" rmng:"+rmngsteps);
        unwlkd=unwlkd-usein;

        if (chnksteps>rmngsteps){ chnksteps=rmngsteps; }
        if ((chnksteps==0)&&(insamps.length==0)){ chnksteps=rmngsteps; } 

        if (tumtime&&(chnksdn>2))
        { long tumstop=System.currentTimeMillis();
					long tumplt=((tumstop-tumstrt)*pltLen)/chnksdn;
					if(tumplt>5000)
					{ poutln("  Plot could take more than "+(tumplt/1000)+" seconds"); }
					tumtime=false; 
				}
			}
      rsltAry=Foamer.Minspect(insamps, gscale, stepsz , chnksteps);
      chnksdn+=chnksteps;
      /*  
      poutln("\nDone microSame:"
            +"  insams:"+insamps.length
            +"  unwlkd:"+unwlkd
            +"  stpslf:"+rmngsteps
            +"  rslta:"+rsltAry.length
            );
      */
      graphReslts(rsltAry);
      //if(((oprint++)%otimer)==0) 
      //{ poutln("   "+rmngsteps); }
      }//end of work
    

	  long finn = System.currentTimeMillis(); 
    poutln("\n  Took:"+((finn-timerstrt)/1000)+"s");
	}//end trumco's constructor which is also its ~executive
 /*-------------------------------------*/  
 
  private double ampfind()
  { tookWav.setSkip(strtSpct);
    short[] tmpsmps =tookWav.getchn(chnlScan);
    int p=tmpsmps[0]; int q=0; long ester=0; long tester=0;
    int window[]= new int[mssrw];
    for(int t=1,e=duraSpct; t<e; t++)
    { if(t==tmpsmps.length) { tmpsmps =tookWav.getchn(chnlScan); e-=t; t=0; }
      q=p;p=tmpsmps[t];
      window[t%mssrw]=Math.abs(p); //Math.abs(p-q)+Math.abs(p+q);
      ester+=window[t%mssrw]-window[(t+1)%mssrw];
      if((t>=mssrw)&&(ester>tester)){ tester=ester; }
		}
    window = null;
    tookWav.setSkip(0);
    //amp(wavderv)=2*amp(wav)*pi/wavlen
    double ret= (double)(tester)/(mssrw*0.64d*0x7fff);
    if(ret<0.01d){ ret=0.1d; }
    if(ret>1d){ ret=1d; }
    return ret;
	}

  private void graphReslts(int[] Viso)
  { for(int nwcol=0 ; nwcol<chnksteps; nwcol++)               //loopthrough new columbs
    { for(int line=0 ; line<pltHgt; line++)                  //loopdown through lines
      { BmapSrc[(line)*(pltLen+axWdth) +Bmapcol+axWdth]=Viso[line*chnksteps+nwcol]; } //
      Bmapcol+=1; rmngsteps-=1;
      if(Bmapcol==pltLen)
      { //poutln("\nCalling save..\n"+"chnksteps:"+chnksteps+" nwcol:"+nwcol);
        saveGraph();   }                                    
		}
	}//end graphReslts
  
  private void saveGraph()
  { if(axHght>8){ drawTimeAx(); }
    if(axWdth>18&&!freqdrawn){ drawFreqAx(); freqdrawn=true; }
    
    String ozz="";
    if(tplots>10&&btmpnum<10){ ozz=ozz+"0"; }
    if(tplots>100&&btmpnum<100){ ozz=ozz+"0"; }
    if(tplots>1000&&btmpnum<1000){ ozz=ozz+"0"; }  
    String tFileOut = filenam+pofix+"."+ozz+btmpnum+".bmp"; btmpnum++;
  
    BmpSav outmip = new BmpSav(BmapSrc ,(pltLen+axWdth) ,(pltHgt+axHght) ,tFileOut);
    Bmapcol=maplap;
    
    if(maplap>0)                                     //overlap output plots
    { for(int line=0 ; line<pltHgt; line++)          //copy end to beginning             
      { for(int nwcol=0 ; nwcol<maplap; nwcol++)                  
        { //poutln(" 1:"+((line)*(pltLen+axWdth)+nwcol+axWdth)
          //      +" 2:"+((line)*(pltLen+axWdth)+pltLen-maplap+nwcol+axWdth));
          BmapSrc[(line)*(pltLen+axWdth)+nwcol+axWdth]= 
          BmapSrc[(line)*(pltLen+axWdth)+pltLen-maplap+nwcol+axWdth]; 
      } } }
    
    if (rmngsteps<(pltLen-maplap))                                  //if last plot is smaller
    { int npltLen=rmngsteps+maplap; freqdrawn=false;                //make and fill smaller array
      int[] tmpbSrc =new int[(pltHgt+axHght)*(npltLen+axWdth)];
      
      for(int line=0 ; line<pltHgt; line++)                   
      { for(int nwcol=0 ; nwcol<npltLen; nwcol++)                  
        { tmpbSrc[(line)*(npltLen+axWdth)+nwcol+axWdth]= 
          BmapSrc[(line)*(pltLen+axWdth)+nwcol+axWdth];
      } } 
      BmapSrc =tmpbSrc;
      pltLen=npltLen;  
		}
	}
  /*-------------------------------------*/  

  /*big chunk of axis drawing code follows (beware)*/
  private void drawFreqAx()   
  { freqdrawn=true;
    //BmapSrc[axWdth]=fvcol;
    //BmapSrc[(pltLen+axWdth)+axWdth]=fvcol;     //draw first 2 pixels of axis
    int scor1=scoreVal(screenScale(2,fsunit)); 
    int scor2=scoreVal(screenScale(3,fsunit));
    int scor0=0, pxfree=2;                      
    
    double eqcalc= 2*pltHgt, bndfac=(double)4*pltHgt/mssrw;
    
    int lip=0; 
    double fe= (pltHgt-eqcalc/screenScale(-1,1));
    double fo= (pltHgt-eqcalc/screenScale(0,1));

    int middy=clrmrg(thblk,thcol,0.42f);
    for( ;(lip<pltHgt)&&((fo-fe)>bndfac/2); )
    { lip++; fe=fo; fo= pltHgt-eqcalc/screenScale(lip,1);
      BmapSrc[(lip)*(pltLen+axWdth)+axWdth-2]
      =clrmrg( 0x00600000,middy, (float)((fo-fe)-bndfac/2)/(float)((fo-fe+bndfac/2)) );
      }      

    fo=(fo/bndfac)%2;
    for(int line=lip; line<pltHgt; line++)
    { int tf=0; fe=fo;
      fo= ((pltHgt-eqcalc/screenScale(line,1))/bndfac)%2;
      if(fe<=1&&fo<=1){ tf=thblk; }
      if(fe>=1&&fo>=1){ tf=thcol; }
      if(fe<1&&fo>1)  { tf=clrmrg(thblk,thcol, (float)((1-fe)/(1-fe+fo-1)) ); }
      if(fe>1&&fo<1)  { tf=clrmrg(thcol,thblk, (float)((2-fe)/(2-fe+fo)) ); }
      int th=(tf>>1)&0x007f7f7f;
      BmapSrc[(line)*(pltLen+axWdth)+axWdth-2]=tf;
      //BmapSrc[(line)*(pltLen+axWdth)+axWdth-1]=th;
      //BmapSrc[(line)*(pltLen+axWdth)+axWdth-3]=th; 
      }//end bar striping    
    
    
    for(int line=2; line<pltHgt; line++)
    { //BmapSrc[(line)*(pltLen+axWdth) +axWdth]=fvcol;              //draw little line
      int spctwk=(int)(Math.sqrt(pltHgt)/10);

      pxfree++; scor0=scor1; scor1=scor2;            

      if(line+2>=pltHgt){ scor2=0; }
      else{ scor2= scoreVal(screenScale(line+2,fsunit)); }

      if( ((pxfree>2)&&(scor0>scor1)&&(scor0*3>scor2*3))
         |((pxfree>3)&&(scor0*3>=scor1*2))
         |((pxfree>4)) )              
      { int tig=BmapSrc[(line)*(pltLen+axWdth)+axWdth-2];
        int tis=clrmrg(tig,tdcol,0.5f);
        BmapSrc[(line)*(pltLen+axWdth) +axWdth-1]=tig;
        BmapSrc[(line)*(pltLen+axWdth) +axWdth-3]=tis;
        BmapSrc[(line)*(pltLen+axWdth) +axWdth-4]=clrmrg(tis,tdcol,0.5f);

        int scrbpos=((line-2)*(pltLen+axWdth)+axWdth-7);
        double scribv=screenScale(line,fsunit);
        //
        int hunit=0;
        int lgstDgt=pmsigdig( scribv ); 

        //System.out.print(scribv+" "+lgstDgt);
        //if lgstdgt > availplaces, or lgstdgt > 4, 
        
        int availplc=(axWdth-5)/4;
        int modplc=(axWdth-5)%4;
        if(lgstDgt<-1){ hunit=2; scribv*=1000; lgstDgt+=3; availplc--;}
        if(lgstDgt>5|lgstDgt>availplc){ hunit=4; scribv/=1000; lgstDgt-=3; availplc--;}
        if(lgstDgt>5|lgstDgt>availplc){ hunit=3; scribv/=1000; lgstDgt-=3; }
 
        if(lgstDgt==0) {lgstDgt=1;} 
        int deplac=0;
        int posidplc=availplc-lgstDgt; //System.out.println(" "+posidplc);
        if(posidplc>0)
        { if(modplc>1){ deplac=posidplc;}
          else{ if(posidplc>1){ deplac=posidplc-1; }}
          }            
        
        double dc=0.5d;
        for(int d=0; d<deplac; d++){ dc/=10; }
        int tg=pmsigdig(scribv);
        
        if(tg==0){tg=1;}
        if (tg<pmsigdig(scribv+dc)){ deplac-=1; }
        if (deplac<0){ scribv-=dc; }
          
        scribVal( tdcol, scrbpos, scribv, deplac, hunit, availplc*4 );  
        pxfree=-(8+spctwk);
        }
      pxfree++;
      }              //for all lines
    return; }        //done drawing freq axis


  private void drawTimeAx()
  { /*poutln(" axWdth:"+axWdth+"(pltHgt+2)*(pltLen+axWdth):"+(pltHgt+2)*(pltLen+axWdth)
          +"\n(pltHgt+axHght)*(pltLen+axWdth):"+(pltHgt+axHght)*(pltLen+axWdth)
          +"\nBmapSrc.length"+BmapSrc.length); */
    
    //clear digit area
    for (int clearg=(pltHgt+2)*(pltLen+axWdth); clearg<(pltHgt+axHght)*(pltLen+axWdth) ;clearg++ )
    { BmapSrc[clearg]=0; }
    for (int clearg=(pltHgt)*(pltLen+axWdth)+axWdth; clearg<(pltHgt+1)*(pltLen+axWdth) ;clearg++ )
    { BmapSrc[clearg]=0; BmapSrc[clearg+pltLen+axWdth]=0; }

    double dsamrate=(double)sampyrate;
    double oPxTm=(double)(stepsz/dsamrate)*1000000d;
    double mssrt=mssrw*1000000d/dsamrate;
    double stept=stepsz*500000d/dsamrate;
    double[] markInvls={ 5,10,20,25 };                //stepsz options for times
    double frstGrphSmpl=(double)strtd;                  //sample index of strt of graph
    strtd+=(double)(pltLen-maplap)*stepsz;                //updates for startof next graph
    double goodstep=28000d; boolean mksqueeze=false,mkhop=false;
    if(pltLen<100){ goodstep=14000d; mksqueeze=true; }
    if(pltLen<15){ goodstep=(pltLen*1000)/3.5d+4000d; mksqueeze=true; }
    if(pltLen<3){ goodstep=15000; mksqueeze=false; }
    if(pltLen==1){ goodstep=2500; mksqueeze=false; }
    double minMrkI=(double)(stepsz*goodstep/dsamrate)*1000;     //nanoseconds elapsed in 25 pixels

    double mrkScl=1d;
    int markChu=0;
    for( ; markInvls[markChu]*mrkScl<minMrkI && mrkScl<1.0e12d ;  )
    { markChu++; if(markChu==4){ markChu=0; mrkScl*=10d;} }

    double mrkInrvl= markInvls[markChu]*mrkScl;     
		//mrkInrvl is minimum optsize, 22 to 46 pixels long
                                                    //System.out.println("fitspc:"+fitspc);
    int fsdMrk=0;   //first significant digit of optsize
    for(double test=10; mrkInrvl%test==0; test*=10){ fsdMrk++;}
                                                    //System.out.println("fsdMrk:"+fsdMrk);
    long untFct=1; int deplc=0; int tunit=1;                  //nano                    
    if(fsdMrk>0) { untFct=1000; deplc=3-fsdMrk; tunit=2; }      //millisecs
    if(fsdMrk>3) { untFct=1000000; deplc=6-fsdMrk; tunit=3; }     //secs
    if(fsdMrk>8) { untFct=1000000000; deplc=9-fsdMrk; tunit=4; }    //kilosecs
    if(fsdMrk>10){ untFct=1000000000000l; deplc=12-fsdMrk; tunit=5; } //megasecs
    if(deplc<0)  { deplc=0; }

    int tbar=clrmrg(thcol,thblk,0.7f); //measure separation marks 
    if(stepsz>=mssrw*2)
    { tbar=clrmrg( 0x00600000,tbar, (float)(stepsz-mssrw)/(float)(mssrw+stepsz) ); }
 
    //longed to cast fraction \/
    double nxtMrkTm=((long)(((frstGrphSmpl*1000000d/dsamrate)-oPxTm/3)/mrkInrvl+1))*mrkInrvl;
    int lstscrib=-axWdth;
  
    for (int tmlin=0; tmlin<pltLen; tmlin++ )
    { double thsPxTm=(((double)tmlin*stepsz+frstGrphSmpl)/dsamrate)*1000000d;
      
      /*draw time measure bar*/
      if(mssrw>stepsz*2)
      { double fe=(((thsPxTm-stept)/mssrt)+0.5d)%2;
        double fo=(((thsPxTm+stept)/mssrt)+0.5d)%2;
        if(fe<=1&&fo<=1){ tbar=thblk; } //full fracin 0
        if(fe>=1&&fo>=1){ tbar=thcol; } //full fracin 1
        if(fe<1&&fo>1){ tbar=clrmrg(thblk,thcol,(float)((1-fe)/(1-fe+fo-1))); }//fe is in blnk
        if(fe>1&&fo<1){ tbar=clrmrg(thcol,thblk,(float)((2-fe)/(2-fe+fo))); }  //fe is in blnk
        }//end timemeasure bar striping
      BmapSrc[(pltHgt+1)*(pltLen+axWdth) +axWdth+tmlin]=tbar;
      
      /*write vals and droplines*/
      if (Math.abs(nxtMrkTm-thsPxTm)<Math.abs(nxtMrkTm-(thsPxTm+oPxTm)))
      { nxtMrkTm=((long)((thsPxTm+oPxTm)/mrkInrvl+1))*mrkInrvl;
        
        int gt=clrmrg(BmapSrc[(pltHgt+(1))*(pltLen+axWdth) +axWdth+tmlin],tdcol, 0.4f );
        BmapSrc[(pltHgt+(0))*(pltLen+axWdth) +axWdth+tmlin]=gt; 
        BmapSrc[(pltHgt+(2))*(pltLen+axWdth) +axWdth+tmlin]=gt;
        BmapSrc[(pltHgt+(1))*(pltLen+axWdth) +axWdth+tmlin]=gt; 
        //drops a verticle line
      
        if((!mksqueeze)|(!mkhop))
        { BmapSrc[(pltHgt+(3))*(pltLen+axWdth) +axWdth+tmlin]=tdcol;
          scribVal( tdcol, ((pltHgt+3)*(pltLen+axWdth)+axWdth+tmlin-2), 
                      thsPxTm/untFct, deplc, tunit, (tmlin-lstscrib-6) );
          lstscrib=tmlin; 
          mkhop=true; //poutln("  "+thsPxTm/untFct);
				}
        else
        { BmapSrc[(pltHgt+(2))*(pltLen+axWdth) +axWdth+tmlin]=tdcol;
          mkhop=false; }
        //System.out.println("thspx:"+thsPxTm/untFct+"  deplc:"+deplc+" fitspc:"+fitspc);
			}//end val writing
		}                                    //for each horiz pixel
    return; }                              //time axis drawn           
        
  private double screenScale( int line, int uni )
  { double retu=0;
    if(!logfsc){ retu= (double)minWv*sclfac/(sclfac-line) ; }
    else { retu=(double) minWv*Math.pow(sclfac,line); }
    if(uni==0)
    { return (double)sampyrate/retu; } //(hrzt)
    return retu; }

  private int scoreVal( double sval )
  { double scorwv=sval; if((scorwv==Double.POSITIVE_INFINITY)|(scorwv<0)){ return 0; }
    for( ; ((scorwv+0.49d)/1000)<1; scorwv*=10){}   //shift up, to 3 whole digits   
    for( ; ((scorwv+0.49d)/1000)>1; scorwv/=10){}   //shift down, to 3 whole digits  
    long scorwvi=(long)(scorwv+0.499d);             //caste decimalps
    int thscor=0;
    for(int sfac=1; sfac<7; sfac++)                 //crazy numberscoring loop
    { long pete=Math.abs((((scorwvi*sfac)+500)%1000)-500);
      thscor|=10000/(1+pete*pete);
      pete=Math.abs((((scorwvi*sfac)+50)%100)-50);            
      thscor|=1000/(1+pete*pete);
      pete=Math.abs((((scorwvi*sfac)+5)%10)-5); 
      thscor|=100/(1+pete*pete); 
		}
    return thscor; //actractiveness of value to label axis
	}

  private int clrmrg(int cola, int colb, float rata)
  { float ratb=1-rata;
    int r=( ( (int)(((cola>>16)&0xff)*rata)+
              (int)(((colb>>16)&0xff)*ratb) ))&0xff;
    int g=( ( (int)(((cola>>8)&0xff)*rata)+
              (int)(((colb>>8)&0xff)*ratb) ))&0xff;
    int b=( ( (int)(((cola)&0xff)*rata)+
              (int)(((colb)&0xff)*ratb) ))&0xff;
    return (r<<16)+(g<<8)+b;
	}

  private int pmsigdig( double valo )
  { int pmsd=-5;  valo*=1000000d;                        
    for(double test=10; (int)(valo/test)!=0; test*=10d){ pmsd++;}
    return pmsd;
	}

  private void scribVal(int clr, int locat, double scribno, int deplac, int unit, int space)
  { if(space<4){ return; }
    for(int dp=0; dp<deplac; dp++){ scribno*=10; }              //sight significant digits
    int wrkInt=(int)(scribno+0.49d);                            //cast off insignif digits

    if(unit>0){ scribDgt(clr, locat, 11+unit); locat-=4; }      //scrib units if applic
    
    for(int m=0; m<deplac; m++)
    { if(space<4){ scribDgt(clr, locat, 11 ); return; }
      scribDgt(clr, locat, wrkInt%10 ); wrkInt/=10; locat-=4; 
      space-=4;
		} //scrib decimals as applic
    
    if(deplac>0){ scribDgt(clr, locat,10); locat-=2; space-=2; }       //scrib point if applic
    
    for( ; (wrkInt/10)!=0;  )
    { if(space<4){ scribDgt(clr, locat, 11 ); return; }
      scribDgt(clr, locat, wrkInt%10 ); wrkInt/=10; locat-=4; 
      space-=4; } //scrib until penultimate

    if(space>1)
    { scribDgt(clr, locat, wrkInt%10 ); locat-=4;  }                //scrib last or a zero

    return; 
	}//end scribVal

  static final boolean x=true, o=false;
  static final boolean[][][] numerc= //tiny numbers
   {{{o,x,o},{x,o,x},{x,o,x},{x,o,x},{o,x,o}},//0
    {{o,x,o},{x,x,o},{o,x,o},{o,x,o},{o,x,o}},//1
    {{x,x,o},{o,o,x},{o,x,o},{x,o,o},{x,x,x}},//2
    {{x,x,o},{o,o,x},{o,x,x},{o,o,x},{x,x,o}},//3
    {{x,o,o},{x,o,x},{x,x,x},{o,x,o},{o,x,o}},//4
    {{x,x,x},{x,o,o},{x,x,o},{o,o,x},{x,x,o}},//5
    {{o,x,x},{x,o,o},{x,x,o},{x,o,x},{o,x,o}},//6
    {{x,x,x},{o,o,x},{o,o,x},{o,x,o},{o,x,o}},//7
    {{o,x,o},{x,o,x},{o,x,o},{x,o,x},{o,x,o}},//8
    {{o,x,x},{x,o,x},{o,x,x},{o,o,x},{o,o,x}},//9

    {{o,o,o},{o,o,o},{o,o,o},{o,o,o},{o,o,x}},//.
    {{o,o,x},{o,o,x},{o,o,o},{o,o,o},{o,o,o}},//'
   
    {{o,o,o},{o,o,o},{x,o,x},{x,o,x},{x,x,x}},//nano
    {{o,o,o},{o,o,o},{x,x,x},{x,x,x},{x,o,x}},//milli
    {{o,o,o},{o,o,o},{o,x,x},{o,x,o},{x,x,o}},//sec
    {{o,o,o},{o,o,o},{x,o,x},{x,x,o},{x,o,x}}};//k

  private void scribDgt(int clr, int locat, int dgt) //puts dot pattern in bitmapsource
  { for(int vertip=0; vertip<5; vertip++)
    { if(numerc[dgt][vertip][0])
      { BmapSrc[locat-2+(vertip*(pltLen+axWdth))]=BmapSrc[locat-2+(vertip*(pltLen+axWdth))]|clr; }
      if(numerc[dgt][vertip][1])
      { BmapSrc[locat-1+(vertip*(pltLen+axWdth))]=BmapSrc[locat-1+(vertip*(pltLen+axWdth))]|clr; }
      if(numerc[dgt][vertip][2])
      { BmapSrc[locat+(vertip*(pltLen+axWdth))]=BmapSrc[locat+(vertip*(pltLen+axWdth))]|clr; } 
		} }
  
  private static void poutln(String deb)  
  { System.out.println(deb); }

  private static void pout(String deb)  
  { System.out.print(deb); }

  private static String pdouble(int dplc, double deb, int size)  //writing to size, for debug output
  { String sdeb= Double.toString((double)((int)(deb*dplc+0.499d))/(dplc));
    size=size-sdeb.length();
    for(int oa=0; oa<size; oa++) { System.out.print(" ");}
    return sdeb;
	}

  private static String helpnotes =
    "  Help Notes for Trumco: \n"
   +"  Java -jar trumco wavname.wav -%params% %values%\n"
	 +"  Creates a big spectrogram or more of input file\n"
   +"  -pltht :output scanlines (plot height in pixels)\n"
   +"  -pltln : output chart width (plot width in pixels)\n"
   +"  -stspc : start output range at millisecs\n"
   +"  -fnspc : finish output range at millisecs\n"
   +"  -dursp : range output duration in millisecs\n"
   +"  -aplts : auto adjust mleap and pltln to produce %V% plots\n"
   +"  -minhz :min hertz to include (equiv as maxwv)\n"
   +"  -maxhz : max hertz to include (equiv as minwv)\n"
   +"  -minwv : min wavelen to include\n"
   +"  -maxwv : max wavelen to include\n"
	 +"  -mrsmp :measure window, in samples (freq detail~complexity)\n"
   +"  -mrtim : measure window in millisecs\n"
   +"  -lpsmp :measure leap (time detail, in samples)\n"
   +"  -lptim : measure leap in millisecs\n"
   +"  -pxamp :auto max amplitude * a factor \n"
   +"  -mxamp : max amplitude to expect (0.01>1)\n"
   +"  -chnnl :which channel to scan 0,1\n"
   +"  -dwnmx : sum of 2 channels\n"
   +"  -dffmx : difference between 2 channels\n"
   +"  -axisc : change axis\n\n"
   +"  -gamma : change gamma\n\n"
   ;
   
  }//end trumco

    /*
	
	
-aplts : auto adjust mleap and pltln to produce %V% plots\n"
-axisc : change axis\n\n"

-chnnl : which channel to scan 0,1\n"

-dffmx : difference between 2 channels\n"

-dursp : spect duration in millisecs\n"

-dwnmx : sum of 2 channels\n"

-fiwvl"

-fnspc : finish spect at millisecs\n"

-gamma"

-logfs"

-lpsmp : measure leap (time detail, in samples)\n"

-lptim : measure leap in millisecs\n"

-maxhz : max hertz to include (equiv as minwv)\n"

-maxwv : max wavelen to include\n"

-minhz : min hertz to include (equiv as maxwv)\n"

-minwv : min wavelen to include\n"

-mrsmp : measure window, in samples (freq detail~complexity)\n"

-mrtim : measure window in millisecs\n"

-msspc"

-mxamp : max amplitude to expect (0.01>1)\n"
-mxamp"
-mxamp"

-namex" 
-namex" 

-noaxf"
-noaxf"
-noaxt"
-noaxt"

-outfi"
-outfi"

-ovlap"
-ovlap"

-ovrwt"
-ovrwt"

-pltht : scanlines (plot height in pixels)\n"
-pltht"
-pltht"

-pltln : output chart width (plot width in pixels)\n"
-pltln"
-pltln"

-pntdt"
-pntdt"

-pxamp : auto max amplitude * factor \n"
-rleap"
-sdith"
-spred"
-stspc : start spect at millisecs\n"
-stspc
-tprpl
*/
	
	
	
	
	
	
	
	
//writeaval(color, position, number, decplaces, units)
//discMsd(number)
// if markscale is 1, then nanoseconds are appropriate interval quoted as, 5 to 25 etc
// if 10, still appropriate 50 to 250 so are millisecs though, 0.05 to 0.25                                    
// if 100, milliseconds are appropriate - .5 to 2.5
// if 1000 millisecs  5 to 25, secs 0.005 to 0.025
// if 10000 millisec 50 to 250, secs is 0.05 to 0.25
// if 100000 millisec 500 to 2500 na, secs 0.5 to 2.5
// 1000000 secs 5 to 25 -large, 300 seconds /1000 pixels, .3secs per pixel*20 6 seconds -not unherad of
// 10000000 secs 50 250 -danger, but unusualy large for 20 pixels, any kilosecsbetter
// 100000000 kilosecs required, 500 too 2500 , better .5 to 2.5

//so 1: nanosecs, no dp
//10: millisecs   2 dp
//100: millisecs  1 dp
//1000: millisecs no dp
//10000: secs 2dp
//100000: secs 1dp
//1000000: secs no dp
//10000000: kilosecs 2dp
//100000000: kil

//find out where the decimalpoint of the mark interval is
//put it at at least 2

