/****************************************************************************
 ** @license
 ** This file is part of yFiles for HTML 2.1.0.1.
 ** 
 ** yWorks proprietary/confidential. Use is subject to license terms.
 **
 ** Copyright (c) 2018 by yWorks GmbH, Vor dem Kreuzberg 28, 
 ** 72070 Tuebingen, Germany. All rights reserved.
 **
 ***************************************************************************/

import y from'./core.js';import l from'./lang.js';import'./algorithms.js';import'./layout-orthogonal.js';import'./router-polyline.js';import'./router-other.js';import'./layout-tree.js';import'./layout-core.js';(function(a,b,c,d){'use strict';b.lang.addMappings('yFiles-for-HTML-Complete-2.1.0.1-Evaluation (Build 1c6e00a8d772-04/06/2018)',{_$_vqd:['aspectRatio','$ck'],_$_wqd:['gridSpacing','$dk'],_$_jyl:['CompactOrthogonalLayout','UFC'],_$$_vna:['yfiles.orthogonal','yfiles._R.T','yfiles._R.C']});var e=['Illegal value for grid size: ','Aspect ratio must be greater than zero: '];b.lang.module('_$$_vna',function(c){c._$_jyl=new b.lang.ClassDefinition(function(){return{$extends:a.C.WFC,constructor:function(){a.C.WFC.call(this);var b=new a.T.WFC.T2(0,a.XE.$f4);this.$vvV=b;var c=new a.C.TFC;c.$LKV=!0,c.$Af=3,this.$Az=c;var d=new a.T.WFC.T1(null);this.$wvV=d,this.$uvV=a.BNA.$m(null),this.$ck=1,this.$dk=20},$f:0,$f1:0,$f5:0,$f6:0,_$_wqd:{get:function(){return this.$f1},set:function(b){if(b<1)throw a.QE.$m18(e[0]+b);if(this.$f1=b,this.$Az instanceof a.C.TFC){this.$Az.$ek=b}if(this.$wvV instanceof a.T.WFC.T1){var c=this.$wvV;c.$f.$fxV=2*b,c.$f.$Sk=b}var d=this.$uvV;if(d instanceof a.T.WFC.T){d.$f.$eS=new a.C.LGC(0,0,b)}else d instanceof a.CNA&&(d.$f=0.125)}},_$_vqd:{get:function(){return this.$f},set:function(b){if(b<=0)throw a.QE.$m18(e[1]+b);if(this.$f=b,this.$wvV instanceof a.T.WFC.T1){this.$wvV.$f.$SpV=new a.C.GRA(b,1)}this.$uvV instanceof a.CNA&&(this.$uvV.$f=0.125)}}}})})}(y.lang.module('yfiles._R'),y));export const CompactOrthogonalLayout=y.orthogonal.CompactOrthogonalLayout;export default y;
