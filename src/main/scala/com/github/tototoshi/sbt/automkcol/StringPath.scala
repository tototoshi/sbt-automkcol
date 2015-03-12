package com.github.tototoshi.sbt.automkcol

/**
 * Convenience class to be able to write a String with '/' in code.
 */
object StringPath {

  class StringPath(val path: String) {
    def / (part: String): String = path +
      (if(path.endsWith("/")) "" else "/") +
      (if(part.startsWith("/")) part.substring(1) else part)

    def asPath: String = "/" + path.replace('.','/')
  }

  implicit def string2StringPath(path: String) = new StringPath(path)

}
