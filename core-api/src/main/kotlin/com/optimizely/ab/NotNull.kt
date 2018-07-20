package com.optimizely.ab

class NotNull {
    private var params:Array<Any?>
    private constructor(vararg args : Any?) {
        params = args as Array<Any?>
    }

    operator fun get(i:Int) : Any {
        return params[i]!!
    }

    companion object {
        fun test(vararg params: Any?) : NotNull? {
           for (p in params) {
               if (p == null) {
                   return null;
               }
           }

            return NotNull(params)
        }
    }

}