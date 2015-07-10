package edu.nus.hipci.common

/**
 * Database entry for test result
 * @param commitID The hash of the commit this test was executed
 * @param configID The id of the config, can be a SHA of the ConfigSchema
 * @param config The ConfigSchema
 */
case class DbEntity(commitID: String, configID: String, config: ConfigSchema)
