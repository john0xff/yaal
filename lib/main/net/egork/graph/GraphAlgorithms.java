package net.egork.graph;

import net.egork.collections.Pair;
import net.egork.collections.comparators.IntComparator;
import net.egork.collections.heap.Heap;
import net.egork.numbers.IntegerUtils;

import java.util.*;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class GraphAlgorithms {
	public static<V> long dinic(Graph<V> graph, V source, V destination) {
		int sourceID = graph.resolve(source);
		int destinationID = graph.resolve(destination);
		int vertexCount = graph.getVertexCount();
		int[] queue = new int[vertexCount];
		int[] distance = new int[vertexCount];
		@SuppressWarnings("unchecked")
		int[] nextEdge = new int[vertexCount];
		long totalFlow = 0;
		while (true) {
			edgeDistances(graph, sourceID, queue, distance);
			if (distance[destinationID] == -1)
				break;
			Arrays.fill(nextEdge, -2);
			totalFlow += dinicImpl(graph, sourceID, destinationID, Long.MAX_VALUE, distance, nextEdge);
		}
		return totalFlow;
	}

	private static<V> void edgeDistances(Graph<V> graph, int sourceID, int[] queue, int[] distance) {
		Arrays.fill(distance, -1);
		distance[sourceID] = 0;
		int size = 1;
		queue[0] = sourceID;
		for (int i = 0; i < size; i++) {
			int current = queue[i];
			int edgeID = graph.firstOutbound[current];
			while (edgeID != -1) {
				if (graph.removed[edgeID] || graph.capacity[edgeID] == 0) {
					edgeID = graph.nextOutbound[edgeID];
					continue;
				}
				int next = graph.to[edgeID];
				if (distance[next] == -1) {
					distance[next] = distance[current] + 1;
					queue[size++] = next;
				}
				edgeID = graph.nextOutbound[edgeID];
			}
		}
	}

	private static<V> long dinicImpl(Graph<V> graph, int sourceID, int destinationID, long flow, int[] distance, int[] nextEdge) {
		if (sourceID == destinationID)
			return flow;
		if (flow == 0 || distance[sourceID] == distance[destinationID])
			return 0;
		int edgeID = nextEdge[sourceID];
		if (edgeID == -2)
			nextEdge[sourceID] = edgeID = graph.firstOutbound[sourceID];
		long totalPushed = 0;
		while (edgeID != -1) {
			int nextDestinationID = graph.to[edgeID];
			if (graph.removed[edgeID] || graph.capacity[edgeID] == 0 || distance[nextDestinationID] != distance[sourceID] + 1) {
				nextEdge[sourceID] = edgeID = graph.nextOutbound[edgeID];
				continue;
			}
			long pushed = dinicImpl(graph, nextDestinationID, destinationID, Math.min(flow, graph.capacity[edgeID]),
				distance, nextEdge);
			if (pushed != 0) {
				graph.edges[edgeID].pushFlow(pushed);
				flow -= pushed;
				totalPushed += pushed;
				if (flow == 0)
					return totalPushed;
			}
			nextEdge[sourceID] = edgeID = graph.nextOutbound[edgeID];
		}
		return totalPushed;
	}

	public static<V> Pair<Map<V, Long>, Map<V, Edge<V>>> dijkstraAlgorithm(Graph<V> graph, V source) {
		Pair<long[], int[]> result = dijkstraAlgorithmByID(graph, graph.resolve(source));
		Map<V, Long> distance = new HashMap<V, Long>();
		Map<V, Edge<V>> last = new HashMap<V, Edge<V>>();
		for (int i = graph.getVertexCount() - 1; i >= 0; i--) {
			if (result.second[i] != -1) {
				distance.put(graph.getVertex(i), result.first[i]);
				last.put(graph.getVertex(i), graph.getEdge(result.second[i]));
			}
		}
		distance.put(source, 0L);
		return Pair.makePair(distance, last);
	}

	public static<V> Pair<Long, List<Edge<V>>> dijkstraAlgorithm(Graph<V> graph, V source, V destination) {
		int sourceID = graph.resolve(source);
		int destinationID = graph.resolve(destination);
		if (sourceID == destinationID)
			return Pair.makePair(0L, Collections.<Edge<V>>emptyList());
		Pair<long[], int[]> result = dijkstraAlgorithmByID(graph, sourceID);
		if (result.second[destinationID] == -1)
			return null;
		List<Edge<V>> path = new ArrayList<Edge<V>>();
		int id = destinationID;
		while (id != sourceID) {
			path.add(graph.getEdge(result.second[id]));
			id = graph.from[result.second[id]];
		}
		Collections.reverse(path);
		return Pair.makePair(result.first[destinationID], path);
	}

	public static<V> Pair<long[], int[]> dijkstraAlgorithmByID(Graph<V> graph, int sourceID) {
		int vertexCount = graph.getVertexCount();
		final long[] distance = new long[vertexCount];
		int[] last = new int[vertexCount];
		Arrays.fill(distance, Long.MAX_VALUE);
		Arrays.fill(last, -1);
		distance[sourceID] = 0;
		if (graph.isSparse()) {
			Heap heap = new Heap(vertexCount, new IntComparator() {
				public int compare(int first, int second) {
					return IntegerUtils.longCompare(distance[first], distance[second]);
				}
			}, vertexCount);
			heap.add(sourceID);
			while (!heap.isEmpty()) {
				int current = heap.poll();
				int edgeID = graph.firstOutbound[current];
				while (edgeID != -1) {
					if (graph.removed[edgeID]) {
						edgeID = graph.nextOutbound[edgeID];
						continue;
					}
					int next = graph.to[edgeID];
					long total = graph.weight[edgeID] + distance[current];
					if (distance[next] > total) {
						distance[next] = total;
						if (heap.getIndex(next) == -1)
							heap.add(next);
						else
							heap.shiftUp(heap.getIndex(next));
						last[next] = edgeID;
					}
					edgeID = graph.nextOutbound[edgeID];
				}
			}
		} else {
			boolean[] visited = new boolean[vertexCount];
			for (int i = 0; i < vertexCount; i++) {
				int index = -1;
				long length = Long.MAX_VALUE;
				for (int j = 0; j < vertexCount; j++) {
					if (!visited[j] && distance[j] < length) {
						length = distance[j];
						index = j;
					}
				}
				if (index == -1)
					break;
				visited[index] = true;
				int edgeID = graph.firstOutbound[index];
				while (edgeID != -1) {
					if (graph.removed[edgeID]) {
						edgeID = graph.nextOutbound[edgeID];
						continue;
					}
					int next = graph.to[edgeID];
					if (visited[next]) {
						edgeID = graph.nextOutbound[edgeID];
						continue;
					}
					long total = graph.weight[edgeID] + length;
					if (distance[next] > total) {
						distance[next] = total;
						last[next] = edgeID;
					}
					edgeID = graph.nextOutbound[edgeID];
				}
			}
		}
		return Pair.makePair(distance, last);
	}
}
