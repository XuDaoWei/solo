/*
 * Solo - A small and beautiful blogging system written in Java.
 * Copyright (c) 2010-present, b3log.org
 *
 * Solo is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *         http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.b3log.solo.processor.console;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.b3log.latke.Keys;
import org.b3log.latke.Latkes;
import org.b3log.latke.http.Cookie;
import org.b3log.latke.http.RequestContext;
import org.b3log.latke.http.Response;
import org.b3log.latke.http.renderer.JsonRenderer;
import org.b3log.latke.ioc.Inject;
import org.b3log.latke.ioc.Singleton;
import org.b3log.latke.service.LangPropsService;
import org.b3log.latke.service.ServiceException;
import org.b3log.solo.model.Common;
import org.b3log.solo.model.Option;
import org.b3log.solo.service.OptionQueryService;
import org.b3log.solo.service.SkinMgmtService;
import org.b3log.solo.util.Skins;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Set;

/**
 * Skin console request processing.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 2.0.0.0, Feb 9, 2020
 * @since 3.5.0
 */
@Singleton
public class SkinConsole {

    /**
     * Logger.
     */
    private static final Logger LOGGER = LogManager.getLogger(SkinConsole.class);

    /**
     * Skin management service.
     */
    @Inject
    private SkinMgmtService skinMgmtService;

    /**
     * Option query service.
     */
    @Inject
    private OptionQueryService optionQueryService;

    /**
     * Language service.
     */
    @Inject
    private LangPropsService langPropsService;

    /**
     * Gets skin.
     * <p>
     * Renders the response with a json object, for example,
     * <pre>
     * {
     *     "sc": boolean,
     *     "skin": {
     *         "skinDirName": "",
     *         "mobileSkinDirName": "",
     *         "skins": "[{
     *             "skinDirName": ""
     *         }, ....]"
     *     }
     * }
     * </pre>
     * </p>
     *
     * @param context the specified request context
     */
    public void getSkin(final RequestContext context) {
        final JsonRenderer renderer = new JsonRenderer();
        context.setRenderer(renderer);

        try {
            final JSONObject skin = optionQueryService.getSkin();
            if (null == skin) {
                renderer.setJSONObject(new JSONObject().put(Keys.STATUS_CODE, false));

                return;
            }

            final Set<String> skinDirNames = Skins.getSkinDirNames();
            final JSONArray skinArray = new JSONArray();
            for (final String dirName : skinDirNames) {
                final JSONObject s = new JSONObject();
                final String name = Latkes.getSkinName(dirName);
                if (null == name) {
                    LOGGER.log(Level.WARN, "The directory [{}] does not contain any skin, ignored it", dirName);

                    continue;
                }

                s.put(Option.ID_C_SKIN_DIR_NAME, dirName);
                skinArray.put(s);
            }
            skin.put("skins", skinArray.toString());

            final JSONObject ret = new JSONObject();
            renderer.setJSONObject(ret);
            ret.put(Option.CATEGORY_C_SKIN, skin);
            ret.put(Keys.STATUS_CODE, true);
        } catch (final Exception e) {
            LOGGER.log(Level.ERROR, e.getMessage(), e);

            final JSONObject jsonObject = new JSONObject().put(Keys.STATUS_CODE, false);
            renderer.setJSONObject(jsonObject);
            jsonObject.put(Keys.MSG, langPropsService.get("getFailLabel"));
        }
    }

    /**
     * Updates the skin by the specified request.
     * <p>
     * Request json:
     * <pre>
     * {
     *     "skin": {
     *         "skinDirName": "",
     *         "mobileSkinDirName": "",
     *     }
     * }
     * </pre>
     * </p>
     *
     * @param context the specified request context
     */
    public void updateSkin(final RequestContext context) {
        final JsonRenderer renderer = new JsonRenderer();
        context.setRenderer(renderer);

        try {
            final JSONObject requestJSONObject = context.requestJSON();
            final JSONObject skin = requestJSONObject.getJSONObject(Option.CATEGORY_C_SKIN);
            final JSONObject ret = new JSONObject();
            renderer.setJSONObject(ret);

            skinMgmtService.updateSkin(skin);

            final Response response = context.getResponse();
            final Cookie skinDirNameCookie = new Cookie(Common.COOKIE_NAME_SKIN, skin.getString(Option.ID_C_SKIN_DIR_NAME));
            skinDirNameCookie.setMaxAge(60 * 60); // 1 hour
            skinDirNameCookie.setPath("/");
            response.addCookie(skinDirNameCookie);
            final Cookie mobileSkinDirNameCookie = new Cookie(Common.COOKIE_NAME_MOBILE_SKIN, skin.getString(Option.ID_C_MOBILE_SKIN_DIR_NAME));
            mobileSkinDirNameCookie.setMaxAge(60 * 60); // 1 hour
            mobileSkinDirNameCookie.setPath("/");
            response.addCookie(mobileSkinDirNameCookie);

            ret.put(Keys.STATUS_CODE, true);
            ret.put(Keys.MSG, langPropsService.get("updateSuccLabel"));
        } catch (final ServiceException e) {
            LOGGER.log(Level.ERROR, e.getMessage(), e);

            final JSONObject jsonObject = new JSONObject().put(Keys.STATUS_CODE, false);
            renderer.setJSONObject(jsonObject);
            jsonObject.put(Keys.MSG, langPropsService.get("updateFailLabel"));
        }
    }

    /**
     * Checks whether the specified input is a non-negative integer.
     *
     * @param input the specified input
     * @return {@code true} if it is, returns {@code false} otherwise
     */
    private boolean isNonNegativeInteger(final String input) {
        try {
            return 0 <= Integer.valueOf(input);
        } catch (final Exception e) {
            return false;
        }
    }
}
