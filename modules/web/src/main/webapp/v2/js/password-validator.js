/*
 * Copyright © 2018 Logistimo.
 *
 * This file is part of Logistimo.
 *
 * Logistimo software is a mobile & web platform for supply chain management and remote temperature monitoring in
 * low-resource settings, made available under the terms of the GNU Affero General Public License (AGPL).
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.  If not, see
 * <http://www.gnu.org/licenses/>.
 *
 * You can be released from the requirements of the license by purchasing a commercial license. To know more about
 * the commercial license, please contact us at opensource@logistimo.com
 */

/**
 * Created by mohan on 17/08/18.
 */

function validatePassword(password, role, user) {
    if(!password || !role || !user) {
        return;
    }

    if(user == password) {
        return "Your password cannot be the same as the username."
    }

    var NUMBER_PATTERN = '(?=.*[0-9])';
    var CHARACTER_PATTERN = '(?=.*[a-zA-Z])';
    var LOWER_PATTERN = '(?=.*[a-z])';
    var UPPER_PATTERN = '(?=.*[A-Z])';
    var SPECIAL_PATTERN = "(?=.*[ !\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~])";
    var isAdmin = role.endsWith('su') || role.endsWith('do');

    var html;
    if (isAdmin) {
        var regExp = new RegExp('^' + NUMBER_PATTERN + LOWER_PATTERN + UPPER_PATTERN + SPECIAL_PATTERN + '.{8,}$');
        if (!regExp.test(password)) {
            html = validateLength() + validateNumber() + validateLower() + validateUpper() + validateSpecial();
        }
    } else {
        regExp = new RegExp('^' + NUMBER_PATTERN + CHARACTER_PATTERN + '.{7,}$');
        if (!regExp.test(password)) {
            html = validateLength() + validateNumber() + validateCharacter();
        }
    }
    if (html) {
        html = '<span class="litetext">Password is not strong enough. It should have' + html + '</span>';
    }
    return html;

    function validateLength() {
        var characters = isAdmin ? 8 : 7;
        var text = 'At least ' + characters + ' characters';
        return getHtml(text, password.length < characters);
    }
    function validateNumber() {
        var text = 'At least one number';
        return getHtml(text, !new RegExp(NUMBER_PATTERN).test(password));
    }
    function validateCharacter() {
        var text = 'At least one character';
        return getHtml(text, !new RegExp(CHARACTER_PATTERN).test(password));
    }
    function validateLower() {
        var text = 'At least one lowercase character';
        return getHtml(text, !new RegExp(LOWER_PATTERN).test(password));
    }
    function validateUpper() {
        var text = 'At least one uppercase character';
        return getHtml(text, !new RegExp(UPPER_PATTERN).test(password));
    }
    function validateSpecial() {
        var text = 'At least one special character';
        return getHtml(text, !new RegExp(SPECIAL_PATTERN).test(password));
    }
    function getHtml(text, isInvalid) {
        var sign, color;
        if(isInvalid) {
            sign = '<span style="width: 8px;display: inline-block;">&#10007;</span>';
            color = 'red';
        } else {
            sign = '<span style="width: 8px;display: inline-block;">&#10003;</span>';
            color = 'green';
        }
        return '<br/><span style="color: ' + color + '">' + sign + ' ' + text + '</span>';
    }
}