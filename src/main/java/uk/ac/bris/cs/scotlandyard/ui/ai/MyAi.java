package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import com.sun.source.tree.Tree;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

public class MyAi implements Ai {

	@Nonnull
	@Override
	public String name() {
		return "Tronkus Nullch";
	}

	// returns move with the highest score
	@Nonnull
	@Override
	public Move pickMove(
			@Nonnull Board board,
			Pair<Long, TimeUnit> timeoutPair) {
		ImmutableSet<Move> moves = board.getAvailableMoves();
		ImmutableMap<Move, Integer> movesMap = getMap(moves, board);
		new Minimax().minimax(board,  1).showBranches();
		return getMaxEntry(movesMap).getKey();
	}

	// returns mapping from a move to its score
	private ImmutableMap<Move, Integer> getMap(ImmutableSet<Move> moves, Board board) {
		ImmutableMap.Builder<Move, Integer> mapBuilder = ImmutableMap.builder();
		for (Move move : moves) {
			int score = score(move, board);
			mapBuilder.put(move, score);
		}
		return mapBuilder.build();
	}

	// returns entry in map with the highest score
	private Map.Entry<Move, Integer> getMaxEntry(ImmutableMap<Move, Integer> map) {
		Map.Entry<Move, Integer> maxEntry = null;
		for (Map.Entry<Move, Integer> entry : map.entrySet()) {
			if (maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0) {
				maxEntry = entry;
			}
		}
		//System.out.println(maxEntry.getValue());
		return maxEntry;
	}

	// returns set of detective locations
	private ImmutableSet<Optional<Integer>> getDetectiveLocations(Board board) {
		ImmutableSet.Builder<Optional<Integer>> detectivesLocationsBuilder = ImmutableSet.builder();
		for (Piece piece : board.getPlayers()) {
			if (piece.isDetective()) {
				detectivesLocationsBuilder.add(board.getDetectiveLocation((Piece.Detective) piece));
			}
		}
		return detectivesLocationsBuilder.build();
	}

	private ImmutableSet<Piece> getDetectives(Board board) {
		ImmutableSet.Builder<Piece> detectivesBuilder = ImmutableSet.builder();
		for (Piece piece : board.getPlayers()) {
			if (piece.isDetective()) {
				detectivesBuilder.add(piece);
			}
		}
		return detectivesBuilder.build();
	}

	/*
	public ImmutableSet<Move> getDetectiveMove(Player detective, Board board, Board.GameState gameState) {
		GameSetup setup = gameState.getSetup();
		ImmutableSet<Piece> detectives = getDetectives(board);
		ImmutableSet.Builder<Move> movesBuilder = ImmutableSet.builder();
		for (Move.SingleMove singleMove : makeSingleMoves(setup, detectives, detective, detective.location())) {
			movesBuilder.add(singleMove);
		}
		return movesBuilder.build();
	}

	 */

	// returns distance between two nodes
	private int getDistance(Board board, int start, int end) {
		return BFS.BFS(board.getSetup().graph, start, end);
	}

	// scoring method which chooses the destination with the most adjacent nodes
	private int scoreAdjacentDetectives(Board board, int destination) {
		int nearbyNodes = board.getSetup().graph.adjacentNodes(destination).size();
		return nearbyNodes;
	}

	// scoring method which chooses the destination the furthest away from all detectives
	private int scoreDistanceDetectives(Board board, int destination) {
		int distance = 0;
		ImmutableSet<Optional<Integer>> detectiveLocations = getDetectiveLocations(board);
		for (Optional<Integer> detectiveLocation : detectiveLocations) {
			distance += getDistance(board, destination, detectiveLocation.get());
		}
		return distance;
	}

	// losingMove is true if the destination is adjacent to a detective
	private boolean losingMove(Board board, int destination) {
		for (int node : board.getSetup().graph.adjacentNodes(destination)) {
			boolean adjacentDetective = getDetectiveLocations(board).stream().anyMatch(detectiveLocation ->
					detectiveLocation.get().equals(node));
			if (adjacentDetective) return true;
		}
		return false;
	}

	// returns the score of a move based on the current board
	public int score(Move move, Board board) {
		assert (move.commencedBy() == Piece.MrX.MRX);
		int destination = move.accept(new Move.FunctionalVisitor<>((singleMove -> singleMove.destination),
				doubleMove -> doubleMove.destination2));
		int score = scoreDistanceDetectives(board, destination);
		if (losingMove(board, destination)) return 0;
		return score;
	}


}

