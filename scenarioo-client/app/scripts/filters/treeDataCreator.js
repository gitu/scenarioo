/* scenarioo-client
 * Copyright (C) 2014, scenarioo.org Development Team
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

/**
 * Transforms any nested object and array structure into a tree structure that can be used by our tree directive.
 */
angular.module('scenarioo.filters').filter('scTreeDataCreator', function () {

    function createTreeData(data) {
        if (angular.isUndefined(data)) {
            return undefined;
        }

        if (angular.isString(data)) {
            return { nodeLabel: 'Value', nodeValue: data, childNodes: [  ] };
        }

        return transformNode(data, '');
    }

    function transformNode(node, nodeTitle) {

        var transformedNode = {
            nodeLabel: nodeTitle
        };

        if (angular.isArray(node)) {

        } else if (angular.isObject(node)) {
            transformedNode.childNodes = createObjectChildNodes(node);
        } else if (angular.isString(node)) {
            transformedNode = node;
        }

        return transformedNode;
    }

    function createObjectChildNodes(node) {
        var childNodes = [];

        angular.forEach(node, function (value, key) {
            if (angular.isArray(value)) {
                childNodes.push({
                    nodeLabel: key,
                    childNodes: createArrayChildNodes(value)
                });
            } else if (angular.isObject(value)) {
                childNodes.push({
                    nodeLabel: key,
                    childNodes: createObjectChildNodes(value)
                });
            } else {
                childNodes.push({
                    nodeLabel: key,
                    nodeValue: value
                });
            }
        });

        return childNodes;
    }

    function createArrayChildNodes(array) {
        var childNodes = [];
        angular.forEach(array, function (element) {
            childNodes.push({
                nodeLabel: '',
                childNodes: createObjectChildNodes(element)
            });
        });

        return childNodes;
    }

    return createTreeData;
});
