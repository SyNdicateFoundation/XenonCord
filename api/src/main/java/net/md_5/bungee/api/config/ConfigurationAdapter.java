package net.md_5.bungee.api.config;

import java.util.Collection;
import java.util.Map;

/**
 * This class allows plugins to set their own configuration adapter to load
 * settings from a different place.
 */
public interface ConfigurationAdapter
{

    /**
     * Reload all the possible values, and if necessary cache them for
     * individual getting.
     */
    public void load();

    /**
     * Gets an integer from the specified path.
     *
     * @param path the path to retrieve the integer from
     * @param def the default value
     * @return the retrieved integer
     */
    public int getInt(String path, int def);

    /**
     * Gets a string from the specified path.
     *
     * @param path the path to retrieve the string from.
     * @param def the default value
     * @return the retrieved string
     */
    public String getString(String path, String def);

    /**
     * Gets a boolean from the specified path.
     *
     * @param path the path to retrieve the boolean form.
     * @param def the default value
     * @return the retrieved boolean
     */
    public boolean getBoolean(String path, boolean def);

    /**
     * Get a list from the specified path.
     *
     * @param path the path to retrieve the list form.
     * @param def the default value
     * @return the retrieved list
     */
    public Collection<?> getList(String path, Collection<?> def);

    /**
     * Get the configuration all servers which may be accessible via the proxy.
     *
     * @return all accessible servers, keyed by name
     */
    public Map<String, ServerInfo> getServers();

    /**
     * Get information about all hosts to bind the proxy to.
     *
     * @return a list of all hosts to bind to
     */
    public Collection<ListenerInfo> getListeners();
}
