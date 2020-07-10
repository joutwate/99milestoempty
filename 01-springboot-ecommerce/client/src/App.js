import React, {Component} from 'react';
import {Redirect, Route, Switch} from 'react-router-dom';

import './App.css';
import Login from "./Login";

import AccountRegistration from "./register/AccountRegistration";
import VerifyRegistration from "./register/VerifyRegistration";

class App extends Component {
  render() {
    return (
        <Switch>
          <Route exact path="/login" component={Login}/>
          <Route exact path="/register" component={AccountRegistration}/>
          <Route exact path="/verify/:registrationToken" component={VerifyRegistration}/>
          <Redirect to="/"/>
        </Switch>
    );
  }
}

export default App;
