package utils

import com.nixxcode.jvmbrotli.common.BrotliLoader
import com.nixxcode.jvmbrotli.dec.BrotliInputStream
import com.nixxcode.jvmbrotli.enc.{BrotliOutputStream, Encoder}

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.util.zip.{GZIPInputStream, GZIPOutputStream}

// https://gist.github.com/owainlewis/1e7d1e68a6818ee4d50e

object Gzip {
  def compress(input: String): Array[Byte] = {
    val bos = new ByteArrayOutputStream(input.length)
    val gzip = new GZIPOutputStream(bos)
    gzip.write(input.getBytes())
    gzip.close()
    val compressed = bos.toByteArray
    bos.close()
    compressed
  }

  def decompress(compressed: Array[Byte]): String = {
    val inputStream = new GZIPInputStream(new ByteArrayInputStream(compressed))
    scala.io.Source.fromInputStream(inputStream).mkString
  }
}

object Brotli {
  def compress(input: String): Array[Byte] = {
    BrotliLoader.isBrotliAvailable

    val params = new Encoder.Parameters().setQuality(10);
    val bos = new ByteArrayOutputStream(input.length)
    val stream = new BrotliOutputStream(bos, params, 4096);
    stream.write(input.getBytes())
    stream.close()
    val compressed = bos.toByteArray
    bos.close()
    compressed
  }

  def decompress(compressed: Array[Byte]): String = {
    BrotliLoader.isBrotliAvailable

    val inputStream = new BrotliInputStream(new ByteArrayInputStream(compressed), 4096)
    scala.io.Source.fromInputStream(inputStream).mkString
  }
}
