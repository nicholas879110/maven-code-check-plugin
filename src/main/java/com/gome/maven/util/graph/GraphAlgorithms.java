/*
 * Copyright 2000-2011 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gome.maven.util.graph;

import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.util.Chunk;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author nik
 */
public abstract class GraphAlgorithms {
    public static GraphAlgorithms getInstance() {
        return ServiceManager.getService(GraphAlgorithms.class);
    }

    
    public abstract <Node> List<Node> findShortestPath( Graph<Node> graph,  Node start,  Node finish);

    
    public abstract <Node> List<List<Node>> findKShortestPaths( Graph<Node> graph,  Node start,  Node finish, int k,
                                                                ProgressIndicator progressIndicator);

    
    public abstract <Node> Set<List<Node>> findCycles( Graph<Node> graph,  Node node);

    
    public abstract <Node> List<List<Node>> removePathsWithCycles( List<List<Node>> paths);

    
    public abstract <Node> Graph<Node> invertEdgeDirections( Graph<Node> graph);

    
    public abstract <Node> Collection<Chunk<Node>> computeStronglyConnectedComponents( Graph<Node> graph);

    
    public abstract <Node> Graph<Chunk<Node>> computeSCCGraph( Graph<Node> graph);
}
