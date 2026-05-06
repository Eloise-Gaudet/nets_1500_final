package com.imdb;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import Centralities.Centralities;
import graph_algorithms.TriadicClosure;
import graph_structures.IMDBGraph;
import graph_structures.Person;

@RestController
@RequestMapping("/api")
public class ApiController {

    @Autowired
    private GraphService service;

    // ── DTOs ──────────────────────────────────────────────────────────────────

    static class PersonDto {
        public String id, name;
        public boolean isActor, isDirector;
        static PersonDto from(Person p) {
            PersonDto d = new PersonDto();
            d.id = p.get_id(); d.name = p.getName();
            d.isActor = p.isActor(); d.isDirector = p.isDirector();
            return d;
        }
    }

    static class NeighborDto {
        public String id, name;
        public boolean isActor, isDirector;
        public int sharedTitles;
        public List<String> sharedTitleNames;
        NeighborDto(String id, String name, int sharedTitles) {
            this.id = id; this.name = name; this.sharedTitles = sharedTitles;
        }
    }

    static class PersonDetailDto {
        public String id, name;
        public boolean isActor, isDirector;
        public int degree;
        public List<NeighborDto> neighbors;
    }

    static class RankDto {
        public String id, name;
        public double score;
        RankDto(String id, String name, double score) {
            this.id = id; this.name = name; this.score = score;
        }
    }

    static class PathStepDto {
        public String id, name;
        public List<String> sharedTitleNames;
        PathStepDto(String id, String name, List<String> sharedTitleNames) {
            this.id = id; this.name = name; this.sharedTitleNames = sharedTitleNames;
        }
    }

    static class PathResultDto {
        public boolean found;
        public int hops;
        public List<PathStepDto> path;
        PathResultDto(boolean found, int hops, List<PathStepDto> path) {
            this.found = found; this.hops = hops; this.path = path;
        }
    }

    static class TriadDto {
        public String pivotId, pivotName, bId, bName, cId, cName;
        public int weightAB, weightAC;
        public boolean isStrong;
    }

    static class StatsDto {
        public int personCount;
        StatsDto(int personCount) { this.personCount = personCount; }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Returns cached Centralities when no filters are set, or a fresh one on a filtered graph. */
    private Centralities centralitiesFor(Integer type, String genres, Integer startYear, Integer endYear) {
        if (type == null && (genres == null || genres.isEmpty()) && startYear == null)
            return service.getCentralities();
        String[] genreArr = (genres != null && !genres.isEmpty()) ? genres.split(",") : null;
        IMDBGraph filtered = service.filteredGraph(type, genreArr, startYear, endYear);
        return new Centralities(filtered);
    }

    // ── Endpoints ─────────────────────────────────────────────────────────────

    @GetMapping("/stats")
    public StatsDto stats() {
        return new StatsDto(service.getGraph().size());
    }

    @GetMapping("/search")
    public List<PersonDto> search(@RequestParam(name = "name") String name) {
        IMDBGraph g = service.getGraph();
        List<PersonDto> results = new ArrayList<>();
        for (String id : service.findPersonIds(name)) {
            Person p = g.get_person(id);
            if (p != null) results.add(PersonDto.from(p));
        }
        return results;
    }

    @GetMapping("/persons/{id}")
    public ResponseEntity<PersonDetailDto> personDetail(
            @PathVariable(name = "id") String id,
            @RequestParam(name = "limit", defaultValue = "25") int limit,
            @RequestParam(name = "type", required = false) Integer type,
            @RequestParam(name = "genres", required = false) String genres,
            @RequestParam(name = "startYear", required = false) Integer startYear,
            @RequestParam(name = "endYear", required = false) Integer endYear) {

        String[] genreArr = (genres != null && !genres.isEmpty()) ? genres.split(",") : null;
        IMDBGraph g = service.filteredGraph(type, genreArr, startYear, endYear);
        Person p = g.get_person(id);
        if (p == null) return ResponseEntity.notFound().build();

        // Build sorted neighbor list
        List<NeighborDto> neighborList = new ArrayList<>();
        for (Map.Entry<String, Integer> e : g.get_neighbors(id).entrySet()) {
            Person n = g.get_person(e.getKey());
            if (n != null) {
                NeighborDto nd = new NeighborDto(e.getKey(), n.getName(), e.getValue());
                nd.isActor = n.isActor(); nd.isDirector = n.isDirector();
                neighborList.add(nd);
            }
        }
        neighborList.sort((a, b) -> b.sharedTitles - a.sharedTitles);
        if (neighborList.size() > limit) neighborList = neighborList.subList(0, limit);
        for (NeighborDto n : neighborList) n.sharedTitleNames = g.get_shared_titles(id, n.id);

        PersonDetailDto dto = new PersonDetailDto();
        dto.id = id; dto.name = p.getName();
        dto.isActor = p.isActor(); dto.isDirector = p.isDirector();
        dto.degree = g.get_degree(id);
        dto.neighbors = neighborList;
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/centrality/degree")
    public List<RankDto> degreeCentrality(
            @RequestParam(name = "topN", defaultValue = "10") int topN,
            @RequestParam(name = "actorsOnly", defaultValue = "false") boolean actorsOnly,
            @RequestParam(name = "directorsOnly", defaultValue = "false") boolean directorsOnly,
            @RequestParam(name = "type", required = false) Integer type,
            @RequestParam(name = "genres", required = false) String genres,
            @RequestParam(name = "startYear", required = false) Integer startYear,
            @RequestParam(name = "endYear", required = false) Integer endYear) {
        List<RankDto> out = new ArrayList<>();
        for (Centralities.PersonRank r : centralitiesFor(type, genres, startYear, endYear).rankByDegree(topN, actorsOnly, directorsOnly))
            out.add(new RankDto(r.personId, r.personName, r.score));
        return out;
    }

    @GetMapping("/centrality/eigenvector")
    public List<RankDto> eigenvectorCentrality(
            @RequestParam(name = "topN", defaultValue = "10") int topN,
            @RequestParam(name = "actorsOnly", defaultValue = "false") boolean actorsOnly,
            @RequestParam(name = "directorsOnly", defaultValue = "false") boolean directorsOnly,
            @RequestParam(name = "maxIter", defaultValue = "15") int maxIter,
            @RequestParam(name = "type", required = false) Integer type,
            @RequestParam(name = "genres", required = false) String genres,
            @RequestParam(name = "startYear", required = false) Integer startYear,
            @RequestParam(name = "endYear", required = false) Integer endYear) {
        List<RankDto> out = new ArrayList<>();
        for (Centralities.PersonRank r : centralitiesFor(type, genres, startYear, endYear).rankByEigenvector(topN, actorsOnly, directorsOnly, maxIter))
            out.add(new RankDto(r.personId, r.personName, r.score));
        return out;
    }

    @GetMapping("/centrality/katz")
    public List<RankDto> katzCentrality(
            @RequestParam(name = "topN", defaultValue = "10") int topN,
            @RequestParam(name = "actorsOnly", defaultValue = "false") boolean actorsOnly,
            @RequestParam(name = "directorsOnly", defaultValue = "false") boolean directorsOnly,
            @RequestParam(name = "alpha", defaultValue = "0.005") double alpha,
            @RequestParam(name = "beta", defaultValue = "1.0") double beta,
            @RequestParam(name = "maxIter", defaultValue = "15") int maxIter,
            @RequestParam(name = "type", required = false) Integer type,
            @RequestParam(name = "genres", required = false) String genres,
            @RequestParam(name = "startYear", required = false) Integer startYear,
            @RequestParam(name = "endYear", required = false) Integer endYear) {
        List<RankDto> out = new ArrayList<>();
        for (Centralities.PersonRank r : centralitiesFor(type, genres, startYear, endYear).rankByKatz(topN, actorsOnly, directorsOnly, alpha, beta, maxIter))
            out.add(new RankDto(r.personId, r.personName, r.score));
        return out;
    }

    @GetMapping("/path")
    public PathResultDto path(
            @RequestParam(name = "from") String from,
            @RequestParam(name = "to") String to,
            @RequestParam(name = "type", required = false) Integer type,
            @RequestParam(name = "genres", required = false) String genres,
            @RequestParam(name = "startYear", required = false) Integer startYear,
            @RequestParam(name = "endYear", required = false) Integer endYear) {
        String[] genreArr = (genres != null && !genres.isEmpty()) ? genres.split(",") : null;
        IMDBGraph g = service.filteredGraph(type, genreArr, startYear, endYear);
        Optional<List<Centralities.PersonStep>> result =
            centralitiesFor(type, genres, startYear, endYear).findDegreesOfSeparation(from, to);
        if (!result.isPresent()) return new PathResultDto(false, -1, new ArrayList<>());
        List<Centralities.PersonStep> raw = result.get();
        List<PathStepDto> steps = new ArrayList<>();
        for (int i = 0; i < raw.size(); i++) {
            Centralities.PersonStep s = raw.get(i);
            List<String> titles = (i > 0) ? g.get_shared_titles(raw.get(i - 1).personId, s.personId) : new ArrayList<>();
            steps.add(new PathStepDto(s.personId, s.personName, titles));
        }
        return new PathResultDto(true, steps.size() - 1, steps);
    }

    @GetMapping("/triadic/{id}")
    public List<TriadDto> triadicForPerson(
            @PathVariable(name = "id") String id,
            @RequestParam(name = "limit", defaultValue = "25") int limit,
            @RequestParam(name = "strongOnly", defaultValue = "false") boolean strongOnly) {

        IMDBGraph g = service.getGraph();
        List<TriadicClosure.OpenTriad> triads = TriadicClosure.findOpenTriadsForNode(g, id);

        List<TriadDto> out = new ArrayList<>();
        for (TriadicClosure.OpenTriad t : triads) {
            if (strongOnly && !t.isStrong) continue;
            TriadDto dto = new TriadDto();
            Person pivot = g.get_person(t.pivot);
            Person pB    = g.get_person(t.nodeB);
            Person pC    = g.get_person(t.nodeC);
            dto.pivotId   = t.pivot;
            dto.pivotName = pivot != null ? pivot.getName() : t.pivot;
            dto.bId       = t.nodeB;
            dto.bName     = pB != null ? pB.getName() : t.nodeB;
            dto.cId       = t.nodeC;
            dto.cName     = pC != null ? pC.getName() : t.nodeC;
            dto.weightAB  = t.weightAB;
            dto.weightAC  = t.weightAC;
            dto.isStrong  = t.isStrong;
            out.add(dto);
            if (out.size() >= limit) break;
        }
        return out;
    }
}
