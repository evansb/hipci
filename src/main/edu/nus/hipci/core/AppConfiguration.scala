package edu.nus.hipci.core

import java.nio.file.Paths

/**
 * Models Application Configuration
 * @param projectDirectory The HIP/SLEEK project directory where the executables are located.
 * @param hipDirectory HIP base test directory
 * @param sleekDirectory SLEEK base test directory
 * @param timeout Timeout per test
 */
case class AppConfiguration (
  projectDirectory : String = ".",
  hipDirectory : String = Paths.get("examples", "working", "sleek").toString,
  sleekDirectory : String = Paths.get("examples", "working", "hip").toString,
  timeout: Long = 10000
)

