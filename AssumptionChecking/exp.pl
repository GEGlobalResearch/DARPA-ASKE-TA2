/**********************************************************************
* Note: This license has also been called the "New BSD License" or
* "Modified BSD License". See also the 2-clause BSD License.
*
* Copyright © 2018-2019 - General Electric Company, All Rights Reserved
*
* Project: KApEESH, developed with the support of the Defense Advanced
* Research Projects Agency (DARPA) under Agreement  No.  HR00111990007.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions are met:
* 1. Redistributions of source code must retain the above copyright notice,
*    this list of conditions and the following disclaimer.
*
* 2. Redistributions in binary form must reproduce the above copyright notice,
*    this list of conditions and the following disclaimer in the documentation
*    and/or other materials provided with the distribution.
*
* 3. Neither the name of the copyright holder nor the names of its
*    contributors may be used to endorse or promote products derived
*    from this software without specific prior written permission.
*
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
* THE POSSIBILITY OF SUCH DAMAGE.
*
***********************************************************************/


:- use_module(library(semweb/rdf11)).
%:- use_module(library(semweb/rdfs)).
:- use_module(library(chr)).

:- dynamic(assumption/2).

:- chr_constraint leq/2.
:- chr_constraint lt/2.
:- chr_constraint geq/2.
:- chr_constraint gt/2.
:- chr_constraint eq/2, neq/2.
:- chr_constraint constraint/1, constr/3.
:- chr_constraint unsatisfied/0.
:- chr_constraint trace/1.

%:-set_prolog_flag(chr_toplevel_show_store,false).

:- op(500, xfx, lt).
:- op(500, xfx, leq).
:- op(500, xfx, gt).
:- op(500, xfx, geq).
:- op(500, xfx, eq).
:- op(500, xfx, neq).


:- rdf_register_prefix(imp,'http://sadl.org/sadlimplicitmodel#').
:- rdf_register_prefix(sci,'http://aske.ge.com/sciknow#').
:- rdf_register_prefix(dbn,'http://aske.ge.com/dbnnodes#').
:- rdf_register_prefix(hyp2,'http://aske.ge.com/hypersonicsV2#').
:- rdf_register_prefix(hyp,'http://aske.ge.com/hypersonics#').
:- rdf_register_prefix(sci2,'http://sadl.org/ScientificConcepts2.sadl#').
:- rdf_register_prefix(mm,'http://aske.ge.com/metamodel#').
:- rdf_register_prefix(cg, 'http://aske.ge.com/compgraphmodel#').
:- rdf_register_prefix(list, 'http://sadl.org/sadllistmodel#').

%:- rdf_load('Hypersonics_v2.owl').
%:- rdf_load('ScientificConcepts2.owl').
%:- rdf_load('ScienceKnowledge.owl').
%:- consult('models.pl').

%Split rdf literal 
literal(Value^^Type,Value,Type).

ground_value(X,X) :- number(X).
ground_value(L,V) :-
    literal(L,V,_),
    ground(V).

%% Some rules from examples at https://dtai.cs.kuleuven.be/CHR/about.shtml

X leq Y <=> ground_value(X,VX), ground_value(Y,VY) | VX @=< VY.


reflexivity  @ X leq X, trace(T) <=> true, trace([true|T]).
antisymmetry @ X leq Y , Y leq X, trace(T) <=> X eq Y, trace([X eq Y | T]).
%transitivity @ X leq Y , Y leq Z ==> X leq Z.
% More efficient version of transitivity
transitivity @ X leq Y , Y leq Z ==> X \== Y, Y \== Z, X \== Z | X leq Z.
%idempotence  @ X leq Y \ X leq Y <=> true.
% More efficient
subsumption  @ X leq N \ X leq M <=> ground_value(N,VN), ground_value(M,VM), VN @< VM | true.
subsumption  @ M leq X \ N leq X <=> ground_value(N,VN), ground_value(M,VM), VN @< VM | true.


%X lt Y <=> ground_value(X,VX), ground_value(Y,VY), VX @< VY | true.
%X lt Y <=> ground_value(X,VX), ground_value(Y,VY), VY @=< VX | false.

%X lt Y <=> ground_value(X,VX), ground_value(Y,VY) | VX @< VY.
X lt Y <=> ground_value(X,VX), ground_value(Y,VY), VX @< VY | true.
X lt Y <=> ground_value(X,VX), ground_value(Y,VY), VY @=< VX | false.

X lt X <=> false.
%X lt Y, Y lt X <=> false. Don't need?
transitivity @ X lt Y , Y lt Z  ==> X \== Y, Y \== Z | X lt Z.
transitivity @ X leq Y, Y lt Z  ==> X \== Y, Y \== Z | X lt Z.
transitivity @ X lt Y,  Y leq Z ==> X \== Y, Y \== Z | X lt Z.

%X lt Y, Y eq Z ==> X \== Y | X lt Z.
%X leq Y, Y eq Z ==> Y \== Z, X leq Z.


subsumption  @ X lt Y \ X leq Y <=> true.

subsumption  @ X lt N \ X lt M <=> ground_value(N,VN), ground_value(M,VM), VN @< VM | true.
subsumption  @ M lt X \ N lt X <=> ground_value(N,VN), ground_value(M,VM), VN @< VM | true.

subsumption  @ X leq N \ X lt M  <=> ground_value(N,VN), ground_value(M,VM), VN @< VM | true.
subsumption  @ M leq X \ N lt X  <=> ground_value(N,VN), ground_value(M,VM), VN @< VM | true.
subsumption  @ X lt N  \ X leq M <=> ground_value(N,VN), ground_value(M,VM), VN @< VM | true.
subsumption  @ M lt X  \ N leq X <=> ground_value(N,VN), ground_value(M,VM), VN @< VM | true.

%%%%
subsumption  @ X eq N \ X leq M  <=> ground_value(N,VN), ground_value(M,VM) | VN @=< VM.

%%subsumption  @ X eq M \ N leq X  <=> ground_value(N,VN), ground_value(M,VM) | VN @=< VM.
subsumption  @ X eq M \ N leq X, trace(T)  <=> ground_value(N,VN), ground_value(M,VM), VN @=< VM | trace([VN leq VM |T]).
subsumption  @ X eq M \ N leq X, trace(T)  <=> ground_value(N,VN), ground_value(M,VM), VN @> VM | unsatisfied, trace([VN lt VM, X eq M, N leq X |T]).

subsumption  @ X eq N \ X lt M, trace(T)   <=> ground_value(N,VN), ground_value(M,VM), VN @< VM | trace([VN lt VM |T]).
subsumption  @ X eq N \ X lt M, trace(T)   <=> ground_value(N,VN), ground_value(M,VM), VN @>= VM | unsatisfied, trace([VN lt VM, X eq N, X lt M |T]).

%subsumption  @ X eq M \ N lt X   <=> ground_value(N,VN), ground_value(M,VM) | VN @< VM.
subsumption  @ X eq M \ N lt X, trace(T)   <=> ground_value(N,VN), ground_value(M,VM), VN @< VM | trace([VN lt VM |T]).
subsumption  @ X eq M \ N lt X, trace(T)   <=> ground_value(N,VN), ground_value(M,VM), VN @>= VM | unsatisfied, trace([N lt M, X eq M, N lt X |T]).

subsumption  @ X eq N \ Y leq X  <=> ground_value(N,VN), \+ ground_value(Y,_) | Y leq VN.
subsumption  @ X eq N \ Y lt X   <=> ground_value(N,VN), \+ ground_value(Y,_) | Y lt VN.


/* eq */
built_in     @ X eq Y, trace(T) <=> ground_value(X,VX), ground_value(Y,VY), VX = VY | true,  trace([true |T]).
built_in     @ X eq Y, trace(T) <=> ground_value(X,VX), ground_value(Y,VY), VX \== VY | false, trace([false |T]).
reflexivity  @ X eq X <=> true. 

subsumption  @ X eq Y  \ Y eq X <=> true.
%subsumption  @ X eq Y  \ X leq Y <=> true.
subsumption  @ X eq Y  \ Y leq X <=> true.

%subsumption @  X eq Y \  


/* neq */
built_in     @ X neq Y <=> ground_value(X,VX),ground_value(Y,VY) | VX \== VY.
irreflexivity@ X neq X <=> fail. 

subsumption  @ X neq Y \ Y neq X <=> true.
subsumption  @ X lt Y  \ X neq Y <=> true.
subsumption  @ X lt Y  \ Y neq X <=> true.

simplification @ X neq Y, X leq Y <=> X lt Y. 
simplification @ Y neq X, X leq Y <=> X lt Y. 



X geq Y <=> Y leq X.
X gt Y <=> Y lt X.

% Find a constraint in the constraint store. 
% find_chr_constraint(X leq Y)


sub(_,_,T1,T2) :- var(T1), T2 = T1.
sub(X1,X2,T1,T2) :- \+ var(T1), T1 = X1, T2 = X2, nb_getval(var_map,OldMap), nb_setval(var_map, [X1=X2|OldMap]).
sub(X1,X2,T1,T2) :- \+ T1 = X1, T1 =..[F|L1], sub_list(X1,X2,L1,L2),
                    T2 =..[F|L2].
sub_list(_,_,[],[]).
sub_list(X1,X2,[T1|L1],[T2|L2]) :- sub(X1,X2,T1,T2), sub_list(X1,X2,L1,L2).

%:- nb_setval(var_map,[]).
%add_var_map(M):-
%    nb_getval(var_map, OldMap),
%    nb_setval(var_map, [M|OldMap]).


list_member(M,List):-
	rdf(List, list:first, M),!.
list_member(M,List):-
	rdf(List, list:rest, RList),
	list_member(M, RList).


argument(Eq,S,P,O) :-
   rdf(Eq, rdf:type, imp:'Equation'),
   rdf(Eq, imp:arguments, AL),
   %rdf_member(AO,AL),
   list_member(AO,AL),
   rdf(AO,imp:augmentedType, T),
   rdf(T,imp:constraints, CL),
   rdf_member(C,CL),
   rdf(C,imp:gpSubject,S),
   rdf(C,imp:gpPredicate,P),
   rdf(C,imp:gpObject,O),
   P \== 'http://www.w3.org/1999/02/22-rdf-syntax-ns#type'.


assumption(Eq,Var,Oper,Val,VP):-
   rdf(Eq, rdf:type, imp:'Equation'),
   rdf(Eq, imp:arguments, AL),
   %rdf_member(AO,AL),
   list_member(AO,AL),
   rdf(AO, imp:augmentedType, T),
   rdf(T, imp:constraints, CL),
   rdf_member(FP,CL),
   rdf(FP,rdf:type, imp:'FunctionPattern'),
   rdf(FP,imp:argValues,FPL),
   rdf_member(Var,FPL),
   rdf(Var,rdf:type,imp:'GPVariable'),
   rdf_member(VL,FPL),
   rdf(VL,rdf:type,imp:'GPLiteralValue'),
   rdf(VL,imp:gpLiteralValue,Val),
   rdf(FP,imp:builtin,Oper),
   argument(Eq,_,VP,Var).


%constraint(Eq,Var,Oper,Val):-
%    assumption(Eq,Vr,Op,Vl),
%    localName(Vr,Var),
%    operMap(Op,Oper),
%    literal(Vl,Val,_).
constraint(Eq,Var,Oper,Val):-
    assumption(Eq,_,Op,Vl,P),
    %localName(P,Var),
    rdf(P,rdfs:range,PClass),
    localName(PClass,Var),
    operMap(Op,Oper),
    literal(Vl,Val,_).

operMap('http://sadl.org/builtinfunctions#lessThan',lt).
operMap('http://sadl.org/builtinfunctions#le',leq).
operMap('http://sadl.org/builtinfunctions#greaterThan',gt).
operMap('http://sadl.org/builtinfunctions#ge',geq).    


%model_equations(m1, 'http://aske.ge.com/hypersonicsV2#st_temp_eq_tropo').
%model_equations(m1, 'http://aske.ge.com/hypersonicsV2#st_temp_eq_lowerStrato').
%model_equations(m2, 'http://aske.ge.com/hypersonicsV2#st_temp_eq_tropo').

model_satisfies_assumptions(M,Result,TraceString) :-
    check_model_assumptions(M),
    find_chr_constraint(unsatisfied) -> ( Result=unsatisfied, find_chr_constraint(trace(Trace)), term_string(Trace,TraceString),!) ;
					(  Result=satisfied, TraceString="" ).


%% model_satisfies_assumptions(M,unsatisfied,TraceString) :-
%%     check_model_assumptions(M),
%%     find_chr_constraint(unsatisfied),
%%     find_chr_constraint(trace(Trace)),
%%     term_string(Trace,TraceString).

%% model_satisfies_assumptions(M,satisfied,"") :-
%%     check_model_assumptions(M),
%%     \+ find_chr_constraint(unsatisfied).

check_model_assumptions(M) :-
    trace([]),
    model_equations(M,_),
    add_assumptions(M),
    add_input_value_constraints(M).

model_equations(CCG,Eq) :-
    rdf(CCG, rdf:type, mm:'CCG'),
    rdf(CCG, mm:subgraph, SG),
    rdf(SG, mm:cgraph, DBN),
    rdf(DBN, cg:hasEquation, Eq).


add_input_value_constraints(M) :-
    findall((Inp,Val), model_inputs(M,Inp,Val), IVs),
    add_input_values(IVs).

add_input_values([]):-!.
add_input_values([(I,V)|IVs]) :-
    constr(I,eq,V),
    add_input_values(IVs).
    

model_inputs(CCG, Input, Value) :-
    rdf(Query, mm:execution, Exec),
    rdf(Exec, mm:compGraph, CCG),
    rdf(Query, mm:input, Var), 
    rdf(Var, rdf:type, InputURI),
    localName(InputURI,Input),
    rdf(Var, imp:value, ValueLit),
    literal(ValueLit, Value,_).

%% model_inputs(CCG, Input, Value) :-
%%     rdf(Query, mm:execution, Exec),
%%     rdf(Exec, mm:compGraph, CCG),
%%     rdf(Query, mm:input, InputProperty), 
%%     rdf(InputProperty, rdfs:range, InputURI),
%%     rdf(Var, rdf:type, InputURI),
%%     localName(InputURI,Input),  %Input is a UnittedQuantity
%%     rdf(Var, imp:value, ValueLit),
%%     literal(ValueLit, Value,_).


model_constraint(M,(X,O,Y)):-
    model_equations(M,Eq),
    constraint(Eq,X,O,Y).

add_assumptions(M) :-
    findall(C, model_constraint(M,C), Cs),
    add_constraints(Cs).

add_constraints([]):-!.
add_constraints([(X,O,Y)|Cs]) :-
    constr(X,O,Y),
    add_constraints(Cs).


constr(X,lt,Y) <=> X lt Y.
constr(X,leq,Y) <=> X leq Y.
constr(X,gt,Y) <=> X gt Y.
constr(X,geq,Y) <=> X geq Y.
constr(X,eq,Y) <=> X eq Y.

test:-
    findall((X,O,Y),fact(X,O,Y),Cs),
    add_constraints(Cs).


localName(IRI,Name) :-
    iri_xml_namespace(IRI,_NS,Name).


%find_chr_constraint(C)

%rdf(S,P,O), P \== 'http://www.w3.org/1999/02/22-rdf-syntax-ns#type'.
%rdf(S,P,O), rdf_global_id(rdf:type,Type), P \== Type.

% Query 
%rdf(hyp:'Altitude', imp:value, 30000).
%rdf(hyp:'StaticTemperature', imp:value, null).

%rdf(v0, rdf:type, sci2:'Air').
%rdf(v0, hyp:altitude, v1).
%rdf(v1, imp:value, 30000).


%rdf(rdf(v0, scicncpts2:staticTemperature, null), sadlimplicitmodel:value, null)
%rdf(rdf(v1, hypersonics:altitude, null), sadlimplicitmodel:value, 30000)
