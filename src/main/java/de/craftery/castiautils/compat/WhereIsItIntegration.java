package de.craftery.castiautils.compat;

import net.minecraft.util.math.BlockPos;
import red.jackf.whereisit.api.SearchResult;
import red.jackf.whereisit.client.render.CurrentGradientHolder;
import red.jackf.whereisit.client.render.Rendering;

import java.util.ArrayDeque;
import java.util.Collection;

public class WhereIsItIntegration {
    public static void highlightBlock(int x, int y, int z) {
        // unfortunately this is very hacky. but it works :)
        Rendering.resetSearchTime();
        Rendering.clearResults();
        SearchResult search = SearchResult.builder(new BlockPos(x, y, z)).build();
        Collection<SearchResult> list = new ArrayDeque<>();
        list.add(search);
        Rendering.addResults(list);

        CurrentGradientHolder.refreshColourScheme();
    }
}
