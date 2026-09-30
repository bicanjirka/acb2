package cz.cvut.fit.acb.associative;

/** One side of a stream, as every step rule sees it: symbols for Buyanovsky's steps, decisions for the mixed ones. */
interface StepPort extends SymbolPort, DecisionPort {
}
