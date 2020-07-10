import React, {Component} from "react";
import {BASE_URL} from "../config/config"
import Image from "react-bootstrap/Image";
import please_wait from "../images/please_wait.gif";
import Row from "react-bootstrap/Row";
import "./VerifyRegistration.css"
import axios from "axios";

export default class VerifyRegistration extends Component {
    constructor(props) {
        super(props);

        this.state = {
            "firstName": "",
            "email": "",
            "secret": "",
            "processing": true
        }
    }

    componentDidMount() {
        let registrationToken = this.props.match.params.registrationToken;

        axios.get(BASE_URL + '/register/' + registrationToken).then(response => {
            this.props.history.push("/login");
        }).catch(error => {
            this.setState({
                "firstName": "",
                "processing": false
            });
            console.log(error);
        })
    }

    render() {
        if (this.state.processing) {
            return (
                <div className="loading">
                    <Row>
                        <Image src={please_wait}/>
                    </Row>
                </div>
            );
        }

        if (!this.state.processing && this.state.firstName === "") {
            // Either the verification token was incorrect or something went wrong. Bundling up these two cases into
            // one for now.
            return (
                <div className={"loading"}>
                    <Row>
                        <p>Something went wrong with your request...</p>
                    </Row>
                </div>
            )
        }
    }
}