package com.wing.apirecord.core.filter;

import com.wing.apirecord.core.record.Message;

import java.util.ArrayList;
import java.util.List;

public class FilterChain implements Filter{
   static List<Filter> filters = new ArrayList<Filter>();

    public static List<Filter> addFilter(Filter f) {
        filters.add(f);
        return filters;
    }

    public static void clear() {
        filters.clear();
    }

    private FilterChain(){}

    public static FilterChain getFilterChain() {
        return new FilterChain();
    }

    int index=0;


    @Override
    public boolean doFilter(Message message, FilterChain chain) {

        if(index == filters.size()) return false ;

        Filter f = filters.get(index++);
        return f.doFilter(message, chain);
    }
}
