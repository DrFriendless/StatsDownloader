/**
 * Created by john on 8/10/16.
 */
'use strict';

var app = angular.module('access', ['ngSanitize']);

function AccessCtrl($scope, $http, $timeout, $interval) {
    var vm = this;  
    
    vm.loadData = function() {
        $http({
            method: 'GET',
            url: '/json/access'
        }).success(function(data, status) {
            vm.periods = data;
        });        
    };
    vm.loadData();
}

app.controller('AccessCtrl', AccessCtrl);