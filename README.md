# nets_1500_final

## Project Name & Description

This project is a network analysis tool for exploring relationships within the film industry using actor-movie collaboration data. The application models actors as nodes and shared film appearances as edges, enabling users to investigate structural properties of the Hollywood collaboration network. Users can interactively query the graph to discover connections between actors, identify influential figures, and uncover complex connectivity patterns. The tool combines classical graph algorithms with network metrics to provide both quantitative and visual insights into how the film industry is structured.

## Categories Used

- **Graph and graph algorithms** — the core of the project is a graph of actors connected by shared film appearances, with algorithms including Dijkstra's shortest path, triadic closure, and centrality measures (degree and Katz centrality)
- **Social Networks** — the actor-movie collaboration network is inherently a social network. We analyze it using classic social network concepts such as degrees of separation, clustering (triadic closure), and node importance (centrality)

## Work Breakdown

| Team Member | Contributions |
|---|---|
| **Eloise** | Data parsing, graph design, and filtering (the `graph_structures/` folder) |
| **Henry** | Graph algorithms — triadic closure and Dijkstra's shortest path (`graph_algorithms/` folder) |
| **Tomas** | Centrality measures (degree centrality, Katz centrality) and the UI (`Centralities/` folder) |

## AI Usage

Claude Code was used to assist with structuring portions of the codebase, as well as producing a functioning UI. All algorithms, core logic, and data structures were designed by the team. Specifically, all files/classes in the graph_structures folder were written from scratch, as well as Dijkstra.java, BinaryMinHeap.java, and BinaryMinHeapImpl.java. The other files were constructed using a mixture of hand written code and Claude code.

## Project Update

We changed our project idea from our intial proposal because we thought this new concept would provide more concrete data and allow us to do more interesting graph manipulations. 
