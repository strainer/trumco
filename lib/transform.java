public class transform
/* class to provide discernment methods on pcm stream */
{ 
	private int rsltAry[];  //array used to return results
  private short tstphzff[]; //experimental offsetting of prop phases,to be resolved.. 
 
  private double tstWvln;
  private double minwvl;
  
  private long tprpHtIn, tprpGrIn;
  private int wfmRgbf[]; //waveform ringbuffer, proposals Height, Gradient
  private long prpHtIn[],prpGrIn[];
  private int proppA[][], proppB[][];    //proposal Aphase, proposal Bphase
  private long comHt[][][],comGr[][][];   //combinaz Height, Gradient
  
  private int Mspan, lines, pscale, viswdth; //messerspan, 

  private int itrav, ddnx;
  private byte rcfac, rdfac;    

  static final short[] Isine=
    {   0 , 1608 , 3215 , 4821 , 6423 , 8022 , 9616 , 11204 , 12785 , 14359 , 
    15924 , 17479 , 19024 , 20558 , 22079 , 23586 , 25080 , 26558 , 28021 , 
    29466 , 30894 , 32303 , -31843 , -30474 , -29126 , -27799 , -26496 , 
   -25215 , -23960 , -22729 , -21524 , -20345 , -19194 , -18071 , -16976 ,
   -15911 , -14875 , -13870 , -12896 , -11954 , -11044 , -10166 , -9323 ,
    -8513 , -7737 , -6996 , -6291 , -5621 , -4987 , -4390 , -3830 , -3306 ,
    -2821 , -2373 , -1963 , -1591 , -1258 , -963 , -708 , -491 , -314 ,
     -176 , -77 , -18 , -1  };
      
  private static short Itan[];
  
  private static long pRnd=0x485e2e75d3a763ebl;
  private static int  qRnd=0xcb8c29e3, rRnd=0x73d3db6f , sRnd=0x15da513b;
  
  private static final double dmaxint=(double)0x7fffffff;

  //low digiharmonics, result in sypathetitc matching other frequencies
  //easier to add them straight to rhtWvfms named rhetorical pulse
  //since the space is seemingly free to max, the space is due to be cycled, 
  //at wvlen. windowing seems like choosing arbit points in the cycle to deviate
  //how to deviate, what is deviated is apparently subtracted
  
    /* spot , highest power in sego, throughout whole trianglular*/

  private double prpWvlns[];
                 
  private int ju, ja, rsltClmn;
  private double nyq, colStrt;
  public long totgain, totgain2, rngfll;
  
  private double gamma;
  private int blue,green,red; 
  private int redspn,redstart,redend;
  private long sPLtrav,zPLtrav;
  private int msrStep;
  private double sclfac;         
  private boolean linFqscl;           //linear freq scale
  private double stagger;
  
  private byte[] scrngm;
  
  public transform(int linez, int msre, double scale, double ceil,
                   double gam, double sclfact, boolean logperi, double stag) 
  { 
    linFqscl=!logperi; sclfac=sclfact;
    Mspan=msre; lines=linez;
    
    minwvl=ceil; gamma=gam; sclfac=sclfact;
    
    stagger=stag; colStrt=0; viswdth=0;
    rsltAry = new int[1];
    rngfll=1;
    
    pscale=(int)(scale*0x7fff);
	  
    wfmRgbf  = new int[131072];

    proppA = new int[lines][Mspan]; //proposal definition
    proppB = new int[lines][Mspan]; //must be span+1 to inform its gradients
    comHt= new long[2][2][lines];      
    comGr= new long[2][2][lines];
    
    tstphzff  = new short[lines];
    
    prpHtIn = new long[lines];           //Digitised Integral of rhetoricals 
    prpGrIn = new long[lines];

    scrngm=new byte[1024];          //screengama translation table
    for(double in=0; in<1024; in++)
    { scrngm[(int)in]=(byte)((int)(Math.pow(in/1023,gamma)*255)&0xff); }

    redspn= Mspan;  //limit length of red calculation
    if (Mspan>64)
    { redspn=64+(Mspan-64)/3; }
    redstart=(Mspan-redspn)/2;
    redend=redstart+redspn;
  
    Itan = new short[17];   //initialise for private static Itan method
    for(int i=0; i<16; i++)
    { Itan[i]=(short)(0x0000ffff&(int)(Math.atan(((double)i+0.025f)/16)*2048/Math.PI) ); }
    Itan[16]=(short)(1024); 
    
    prpWvlns =new double[lines];
    double pWvln=(double)minwvl;
    prpWvlns[0]=pWvln;
    for(int i=1; i<lines; i++)
    { if(linFqscl){ pWvln= (double)minwvl*sclfac/(sclfac-i);}
      else{ pWvln*=sclfac; } //implement a hybrid version?
      prpWvlns[i]=pWvln;
      }
    
    /* setup rhetoricals and calc ints*/
    for(int linn=0; linn<lines; linn++)
    { pWvln=prpWvlns[linn];
      
      double midphase=((((double)4096*Mspan-2048)/2)/pWvln)%4096; //inst phase if sin started at tgen=0;
      int shiftphase=(int)(4096-midphase-512+4096.5)%4096; //add this value to make phase=0 at tstsize/2
      tstphzff[linn]=(short)shiftphase;//offsets series by 1/8th of a phase
      
      
      for(int tGenI=0 ; tGenI < Mspan; tGenI++) /* proposal pulse*/
      { //two detection patterns for stock, pscale<7fff, Isine<ffff
        proppA[linn][tGenI]=(short)(((long)Isine((shiftphase+tGenI*4096/pWvln)%4096)*pscale)>>16);
        proppB[linn][tGenI]=(short)(((long)Isine((1024+shiftphase+tGenI*4096/pWvln)%4096)*pscale)>>16);
        }

      prpHtIn[linn]=(long)proppA[linn][0]*proppA[linn][0] /*start tally height*/
                  +(long)proppB[linn][0]*proppB[linn][0];   
      
      //intg self*self * 2
      //in the measuring loop the derivative is assumed as -val[t-1]+val[t] thus begin:
      long Gaa
      =(long)(proppA[linn][0]-(int)(((long)Isine((4096+shiftphase+(-1)*4096/pWvln)%4096)*pscale)/65536));
      long Gbb
      =(long)(proppB[linn][0]-(int)(((long)Isine((1024+4096+shiftphase+(-1)*4096/pWvln)%4096)*pscale)/65536));

      prpGrIn[linn]=Gaa*Gaa+Gbb*Gbb;
      
      for(int tGenI=1 ; tGenI < Mspan; tGenI++)
      { //digitz integral of two stock patterns
        prpHtIn[linn]+=(long)(proppA[linn][tGenI]*proppA[linn][tGenI])   
                      +(long)(proppB[linn][tGenI]*proppB[linn][tGenI]);

        //digi int of quantised derivatives, potentialy twice as large, un-natch
        Gaa=(long)(proppA[linn][tGenI]-proppA[linn][(tGenI-1)]);
        Gbb=(long)(proppB[linn][tGenI]-proppB[linn][(tGenI-1)]);

        prpGrIn[linn]+=Gaa*Gaa+Gbb*Gbb; }

      //this is some sort of hack to mess with grad tallies of low frequencies                    
      //low frequencies will have bad tallies
      //situation is helped by the tally being combination of perpendicular phases
      //explore...
      if(((int)pWvln/2)>Mspan&&linn>10)
      { prpGrIn[linn]=(long)((2*prpHtIn[linn]*Math.PI)/prpWvlns[linn]);
        }

      }//proposal pulses and pulse integral initialisation
    }
  /*endof microsame constructor*/
  
  

  /*make spictures*/  
  public void skid(int skid)
  { rngfll+=skid; }
  
  public int[] Minspect( short[] wfmPtn, double scale, double mstep, int stps )
  {             
    totgain=0; totgain2=0;   //old little perfomance counters
    pscale=(int)(scale*0x7fff);        
      
    if(stps!=viswdth)        //if chunksize has changed, new result array
    { viswdth = stps;
      rsltAry = new int[viswdth*lines]; } 

    /*read waveform portion into ringbuffer*/
    for(int jarp=0; jarp<wfmPtn.length; jarp++)   
    { wfmRgbf[(int)((jarp+rngfll)&0x1ffff)]=(int)wfmPtn[jarp];  
      }
    rngfll+=wfmPtn.length;
    
    int overshoe=(int)(colStrt+Mspan+(mstep*(stps-1))-rngfll);
    
    /*System.out.println("Microsame called\n"
                      +"  rngfll:"+rngfll
                      +"  colStrt:"+colStrt
                      +"  colEnd:"+(int)(colStrt+Mspan-1+mstep*(stps-1))
                      +"  stps:"+stps
                      +"  oshoe:"+overshoe+"\n"
                      );
    */
    
    if(stps==0){ return new int[]{}; } 
  
    if(overshoe>0)
    { for(int jarp=0; jarp<overshoe; jarp++)
      { wfmRgbf[(int)((jarp+rngfll)&0x1ffff)]=0; }
      }

    /* loops: steptime{frequencies{measuretime:measure}{frequencies:test&render}} 
    /* outer loop:  steptime loop) */
    
    double amperv=(double)pscale*Mspan*4;
    double jigi=0d;
    int jiga=0,jigb=0;
		for( int clmn=0; clmn<stps; clmn++) //loop forward in block
    { int thslen=0,msrwlk=0,wfmtrac=0;
      
      //dither factor foolishness, resparklater...
      jiga=jigb;
      jigb=rrandy();
      
      jigi=(double)(jiga+jigb)/(dmaxint*4d); //jigi/3d+jiga/3d+jigb/2;
      if(abs(jigi)>0.42d){ jigi*=0.5d; }
      //System.out.println("! "+jigi);
      if(clmn==0) { jigi=abs(jigi/2); }
      if(clmn==(stps-1)) { jigi=-abs(jigi/2); }
    
      int msrStrt=(int)(colStrt+((mstep)*((double)clmn+jigi*stagger)));
      int Gavr=0,Havr=0;
      
    /* middle loop:  freq loop*/
    for(int linn =0 ; linn < lines; linn++) //loop down through lines
    { 
      int newlen=Mspan;                 //some anigans left over from dynamic measurelength option
      msrwlk=0;                        //(Mspan-thslen)/2; hacks till dynamic window mek done 
      wfmtrac=(msrStrt+msrwlk)&0x1ffff; //
        
      if(newlen!=thslen) //must run at begin of new clmn, or window change
                         //calculates integrals of samples, for level normalisation
      { thslen=newlen;
        int mstop=wfmtrac+thslen;
        Havr=0;Gavr=0;
        for(int trc=wfmtrac; trc<mstop; trc++) { Havr+=wfmRgbf[trc&0x1ffff]; }
        Havr=(Havr+(thslen>>1))/thslen; //(thslen>>1 for 0.5 roundup)
        Gavr=-wfmRgbf[(wfmtrac-1)&0x1ffff] + wfmRgbf[(wfmtrac+thslen)&0x1ffff];
        Gavr=(Gavr+(thslen>>1))/thslen; //resolve if rounding is really bonus here..?
        }
      //Gavr=0;Havr=0;
      long pSaH=0, pSbH=0;  //sumers for height contrasting 
      long pSaG=0, pSbG=0;  //sumers for gradient contrasting
          
      /*inner loop: measuretime loop */
      for( ; msrwlk<thslen; ) //modindex<32768, retindex<end1
      { 
        long nrmls=-Havr+wfmRgbf[wfmtrac]; //normalised sample
        long nrmlG=-Gavr+wfmRgbf[wfmtrac]-wfmRgbf[(wfmtrac-1)&0x1ffff];
        //pserf is sealevel normalised, of wfm
        //Proper Series full
        pSaH+=nrmls*proppA[linn][msrwlk];    //positive,sum,aphase,height
				pSbH+=nrmls*proppB[linn][msrwlk];    //positive,sum,bphase,height 
				
        pSaG+=nrmlG*proppA[linn][msrwlk];    //positive sum aphase gradient
				pSbG+=nrmlG*proppB[linn][msrwlk];
        
        wfmtrac=(wfmtrac+1)&0x1ffff; msrwlk++;        
        }/*inner loop*/

    /*combined volume integral*/
    comHt[0][clmn&1][linn]=pSaH/Mspan;
    comHt[1][clmn&1][linn]=pSbH/Mspan;
    /*combine movement integral*/
    comGr[0][clmn&1][linn]=pSaG/Mspan;
    comGr[1][clmn&1][linn]=pSbG/Mspan;
    
    }/*middle loop lines*/
    
  /* middle loop*/
  /*to reinforce with previous:
  
    int[][] csqHt= new int[2][lines];  //consequetive measurements 
    int[][] csqGr= new int[2][lines];
    
    for(int linn =1 ; linn < lines-1; linn++) //loop down through lines
    { csqHt[0][linn]=(comHt[0][clmn&1][linn-1]+comHt[0][clmn&1][linn+1]+1)>>1; 
      csqHt[1][linn]=(comHt[1][clmn&1][linn-1]+comHt[1][clmn&1][linn+1]+1)>>1; 
      csqGr[0][linn]=(comGr[0][clmn&1][linn-1]+comGr[0][clmn&1][linn+1]+1)>>1; 
      csqGr[1][linn]=(comGr[1][clmn&1][linn-1]+comGr[1][clmn&1][linn+1]+1)>>1; 
      }
    
    for(int linn =0 ; linn < lines; linn++) //loop down through lines
    { comHt[0][clmn&1][linn]=(comHt[0][clmn&1][linn]+csqHt[0][linn])>>1;
      comHt[1][clmn&1][linn]=(comHt[1][clmn&1][linn]+csqHt[1][linn])>>1;
      comGr[0][clmn&1][linn]=(comGr[0][clmn&1][linn]+csqGr[0][linn])>>1;
      comGr[1][clmn&1][linn]=(comGr[1][clmn&1][linn]+csqGr[1][linn])>>1;      
      } */


/*  2nd middle loop*/
    for(int linn =0 ; linn < lines; linn++) //loop down through lines
    { 
    tstWvln=prpWvlns[linn];   //get exact wavelen for line

    tprpGrIn=prpGrIn[linn];   //digitised integral of gradient of this lines propo *2
    tprpHtIn=prpHtIn[linn];   //digitised integral of height of this lines propo *2
     
    //reclaim combasts (contrasts~combinations)      
    long comHx=(long)comHt[0][clmn&1][linn]; //combined Height xphase
    long comHy=(long)comHt[1][clmn&1][linn]; //..xphase
    long comGx=(long)comGr[0][clmn&1][linn]; //combined Gradient xphase
    long comGy=(long)comGr[1][clmn&1][linn]; //..yphase
     
    //(propo is made to gscale, its integral indicates an amplitude of pscale*1
    // so another integral divided by propos' should give amplitude/pscale
    // when coms integral boost is = to half prp int
    // coms amp is same as prps amp      
    // because prpint is made of 2 sine propos at amp of gscale
    // combined contrast integral =Math.sqrt( comHx*comHx + comHy*comHy )/2;
    // amp= pscale* cci / prpi *2;
    // amp(wavderv)=4*amp(wav)/(wavlen*2/pi)
    // amp(wavderv)=2*amp(wav)*pi/wavlen
    
    double ampperhgt=amperv/tprpHtIn; //(*mspan because of storage)
    double amppergrd=amperv/tprpGrIn;
    //amppergrd=(2d*amppergrd*Math.PI)/tstWvln; //works but overdoes high freqs
    //amppergrd=(2d*ampperhgt*Math.PI)/tstWvln;
    //amppergrd=ampperhgt*tstWvln/(2d*Math.PI); //smooth theory, works but fades higher freqs
    //amppergrd=((2d*amppergrd*Math.PI)/tstWvln+ampperhgt*tstWvln/(2d*Math.PI))/2;
    amppergrd=(amppergrd*Math.PI)/tstWvln+ampperhgt*tstWvln/(4d*Math.PI);
    
    //ampergrd=ampergrd*amperhgt/ampergrd
    //int ampHt = (int)(ampperhgt*(abs(comHx) + abs(comHy) ));
    //int ampGd = (int)(amppergrd*(abs(comGx) + abs(comGy) ));
    
    int ampHt = (int)(ampperhgt*Math.sqrt( comHx*comHx + comHy*comHy ));
    int ampGd = (int)(amppergrd*Math.sqrt( comGx*comGx + comGy*comGy ));

    //long ampHt = (long)((Math.sqrt( comHx*comHx + comHy*comHy )*pscale)/(double)tprpHtIn);
    //long ampGd = (long)((Math.sqrt( comGx*comGx + comGy*comGy )*pscale)/(double)tprpGrIn);
    
    int zimap =(Itan( comHy,comHx )+8192+2048+tstphzff[linn])%4096;
    int zimaq =(Itan( comGy,comGx )+8192+tstphzff[linn])%4096; //! fix

    int ampCm=(ampGd+ampHt)/2; //combined amplitude
    int zimac=zimap- ( ( ((zimap-zimaq)%2048)*2-(zimap-zimaq) )*abs(ampGd) )/(abs(ampHt+ampGd)+1)+8192;
  
    int wfmId=0; //Integral of derivative
    int comId=0;
    int aptId=0;
    red=0;
    
    if(ampCm>(pscale>>6))  //weed out low amp2 values, before spending time checking them
    { 
      int aprntAmp=(ampCm); 
      int aprntWlk=redstart;
      
      wfmtrac=(msrStrt+redstart)&0x1ffff;
      
      int aprntPat  =((Isine((aprntWlk<<12)/tstWvln +zimac)>>7) *aprntAmp)>>10;
      int wfmPat    =wfmRgbf[wfmtrac]; 
      int combnPat  =aprntPat+wfmPat;
      
      aprntWlk++; wfmtrac=(wfmtrac+1)&0x1ffff;
      
      for( ; aprntWlk<redend; ) //modindex<32768, retindex<end1
      { int lstwfmPat=wfmPat;
        int lstcombnPat=combnPat;
        int lstaprntPat=aprntPat;
        
        wfmPat=wfmRgbf[wfmtrac];
        aprntPat=(( Isine((aprntWlk<<12)/tstWvln +zimap)>>7) * aprntAmp)>>10;
        combnPat =wfmPat+aprntPat;
      //System.out.println(wfmPat+"  "+aprntPat+" "+combnPat);  
  			wfmId+= Math.abs( wfmPat-lstwfmPat );
        comId+= Math.abs( combnPat-lstcombnPat );
        aptId+= Math.abs( aprntPat-lstaprntPat );
        
        //if((linn==345)&&((int)(msrStrt/mstep)==135))
        //{ System.out.println(thsSb+"\t"+(-(Isine(  ((rti)<<12)/tstWvln +zimac)*ampAp)>>16)+"\t"+thsZi);
        //  }
        wfmtrac=(wfmtrac+1)&0x1ffff; aprntWlk++;        
        }
      //reduction in trav= wfm-tst
      //tstId=wfmId+|-aptId
      //tstId-wfmId=+|-aptId
      //tstId-wfmId/aptId=1 when zero solvency, -1 when full solvency
      long solved=(long)wfmId-comId;
      if(solved>0)
      { red=(int)( ((solved*ampCm)<<9)/((long)pscale*aptId) ) ; }
      
			if(red>1){ red+=Math.sqrt(red+6)*25; }
      //System.out.println(tstPos);
      if(red>1023){ red=1023;}//red=abs(blue-green);
      red=(int)(scrngm[red&1023]&0xff);
      totgain2+=solved; 
      }// clev/98304 = clev/3  /32768  =clev*2 /3  /32768*2  (2 16)
      else{ red=0;}
    
      int blued=(int)((ampGd<<9)/pscale);      
      int greend=(int)((ampHt<<9)/pscale);  //green is currently seen to be more at low freqs

      if(blued>greend){ blued=greend; } 
      if(blued>1023){ blued=1023; }
      if(greend>1023){ greend=1023; }
      
      blue=(int)(scrngm[blued&1023]&0xff);
      green=(int)(scrngm[greend&1023]&0xff);
        
      rsltAry[linn*viswdth+clmn]
				     = ((red<<16)&0xff0000) + ((green<<8)&0xff00) + (blue&0xff);
      //= (((0xff-((green+blue+1)/2))<<16)&0xff0000) 
      //+ (((0xff-((red+blue+1)/2))<<8)&0xff00) 
      //+ ((0xff-((green+red+1)/2))&0xff);
    }/*middle lines*/
    }/*columbs*/

  colStrt+=(mstep*(double)stps);
	return rsltAry;
	}

  private static int rrandy() //jiggled plagerised fast rand generator, silly 
  { pRnd ^= (pRnd << 21); pRnd ^= (pRnd >>> 35); pRnd ^= (pRnd << 4); //Marsaglia 64
    qRnd ^= (qRnd << 17); qRnd ^= (qRnd >>> 13); qRnd ^= (qRnd << 5); //shr3      32
    rRnd = sRnd + rRnd; sRnd = rRnd - sRnd;                           //fib       32.32
    return ((int)pRnd)^(qRnd+rRnd);
    }

  /*the lookup table, returns angle from */  
  private static int Itan(long x, long y)
  { 
    if(x==0){ if(y>0){ return 0; }else{ return 2048; } }
    if(y==0){ if(x>0){ return 1024;   }else{ return -1024; } }

    int inv=1;
    long ax =abs(x),ay =abs(y);

	  if(x<0){ inv=-1 ;} 

		if(y>0){ if(ax>ay){ return inv*(1024 - Itan[(int)((ay<<4)/ax)]); }  //quad 1,4
             else     { return inv*(Itan[(int)((ax<<4)/ay)]); }         //quad 1,4
						 } 
       else{ if(ax>ay){ return inv*(Itan[(int)((ay<<4)/ax)]+1024) ; }  //quad 2,3  
						 else     { return inv*(2048-Itan[(int)((ax<<4)/ay)]); }   //quad 2,3
						 } 
		}//xtan
     
  private static int Isine(double ctrav)
  { 
    //parameter comes in as 0 to 4096, = 0 to 2pi
    //its divided by 16, down to 256 and used to lookup in 64rec segments
    //returns ~17bit twos comp int -
    int itrav=(((int)ctrav)&0xfff); //where pi is 2048, modulate cycle travel
    //ddnx=itrav/16;           //where fracMul is 16, get rounded index
    int ddnx=itrav>>4;     // 4096/16 =256
    int rcfac=(itrav&0x0f);   //reaching factor
    
    //rcfac=itrav%8;          //reaching factor (high when much rounded)
    int rdfac=(16-rcfac);     //rounded  factor (high when little rounded)
    //calced where fracMul is 16

    if(ddnx<64)
    { return (
             (Isine[ddnx]&0x0000ffff)*rdfac   //63
            +(Isine[ddnx+1]&0x0000ffff)*rcfac //64
            +9)>>>4; }
    //returning an int, with 18-2 bits
    //below 128, lookup is straight [ 0:0'1 ... 127:127'128 ], 
    //rounded index is ddnx, reaching index is ddnx+1
    if(ddnx<128)
    { return (
             (Isine[128-ddnx]&0x0000ffff)*rdfac
            +(Isine[127-ddnx]&0x0000ffff)*rcfac 
            +9)>>>4; }
    //here, lookup is reversed [ 256:256'255 ... 511:1'0 ]
    //rounded index is 512-ddnx, reaching index is 511-ddnx  
    if(ddnx<192)
    { return -(
              (Isine[ddnx-128]&0x0000ffff)*rdfac
             +(Isine[ddnx-127]&0x0000ffff)*rcfac
             +9)>>4; }
    //here lookup is negative value of first,
    //_plus 512; [ 512:0'1 ... 767:255'256 ]
    //rounded index is ddnx-512, reaching index is ddnx-511
    return -(
            (Isine[256-ddnx]&0x0000ffff)*rdfac
           +(Isine[255-ddnx]&0x0000ffff)*rcfac
           +9)>>4;   
    //here lookup is negative value of second,
    //[ 768:256'255 ... 1023:1'0 ]
    }//Isin requires Isine[] lookup table, indexes 0 to 127+1(!)

  private static double abs (double d)
  { if(d<0) { return -d; } else { return d; } }

  private static int abs(int value)    //quickie abs smudge, <3 bithacks
  { return (value^(value>>31))-(value>>31); }  

  private static long abs(long value) 
  { return (value^(value>>63))-(value>>63); }

  private static byte signBt(int bog)  //countbits required by value including sign used for scale?
  { byte bat=0;
    if(bog<0){ bog= ((bog<<1)^0xffffffff);} else{bog= (bog<<1); } //reCompliment for counting
    if((bog>>>5)!=0)
    { bog=bog>>>6; bat=6; 
      if((bog>>>5)!=0)
      { bog=bog>>>6; bat=12; } } // a loopstep shortcut cascade

    for(;bog!=0; bat++){ bog=bog>>>1; } //loopsteps
    return bat;
    } 
  
}

/*
version details

integer sine method

acting on delta

Isine( 1024*pu/tstWvln -128+1024)*pscale>>>16 

Math.sin( 2*Math.PI*pu/tstWvln -pif)*pscale

wfmRgbf[(jerp+1)%2048] + blevd*Math.sin( 2*Math.PI*(pu+1)/tstWvln +zimap )

wfmRgbf[(jerp+1)%2048] + (blevd*Isine( 1024*(pu+1)/tstWvln +zimap*512/Math.PI )>>>16)

(long)( (Isine( 1024*(pu+1)/tstWvln +zimaq )*blevd>>>16)+wfmRgbf[(jerp+1)%2048] )

state, difference
height, gradient


*/
    /*
    sort these out sometime!
    mass=amp*time*2/pi; 
    1=amp*time*2/(pi*mass);
    pi*mass=amp*time*2;
    pi*mass/(time*2)=amp;
      
    //pretence area of retorical is length*height*2/PI -no matter what frequency
    //pretence lenght along retorical is length*height*PI/2
    */

//relatively big speed and quality improvements to be made from 
//increasing Isines phase resolution and removing cast of double
