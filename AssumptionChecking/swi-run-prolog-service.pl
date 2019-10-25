% start Prolog Service
:- style_check(-singleton).
:- consult('swi-standard-declarations.pl').


load_pl_file(X) :- consult(X).
unload_pl_file(X) :- unload_file(X).

load_rdf_file(X) :- rdf_load(X).
unload_rdf_file(X) :- rdf_unload(X).

load_into_db(X) :- assertz(X).
load_into_db_beginning(X) :- asserta(X).

retract_all(X) :- retractall(X).


retract_once(X) :- once(retract(X)).


holds(P,S,O) :- rdf(S,P,O).


pred_defined(Pred) :-  
    findall(X,(predicate_property(X1,visible),term_to_atom(X1,X)),XL),
    member(E,XL),
    concat(Pred,'(',PredStr),
    sub_string(E,0,L,A,PredStr), !.


:- consult('swi-custom-predicates.pl').


%%%%%%%%%%%%%%%
% start service
%%%%%%%%%%%%%%%
:- consult('swi-prolog-service.pl').
:- query:port_number(X), server(X), !.
