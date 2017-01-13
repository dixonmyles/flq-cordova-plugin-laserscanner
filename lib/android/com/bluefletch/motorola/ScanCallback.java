package com.bluefletch.motorola;

/**
 * Simple callback interface to use for proxy results.
 *
 * @param <ResultT>
 */
public interface ScanCallback<ResultT> {

    void execute(ResultT result);
}
